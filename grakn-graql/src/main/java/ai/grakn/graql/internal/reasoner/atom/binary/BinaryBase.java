/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.graql.internal.reasoner.atom.binary;

import ai.grakn.graql.VarName;
import ai.grakn.graql.admin.Atomic;
import ai.grakn.graql.admin.ReasonerQuery;
import ai.grakn.graql.admin.VarAdmin;
import ai.grakn.graql.internal.reasoner.atom.Atom;
import ai.grakn.graql.internal.reasoner.atom.predicate.Predicate;
import ai.grakn.graql.internal.reasoner.query.ReasonerQueryImpl;
import ai.grakn.graql.internal.reasoner.rule.InferenceRule;
import ai.grakn.util.ErrorMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static ai.grakn.graql.internal.reasoner.Utility.capture;

/**
 *
 * <p>
 * Base implementationfor binary atoms of the type ($varName, $valueVariable), where value variable
 * references predicates.
 * </p>
 *
 * @author Kasper Piskorski
 *
 */
public abstract class BinaryBase extends Atom {
    private VarName valueVariable;

    protected BinaryBase(VarAdmin pattern, ReasonerQuery par) {
        super(pattern, par);
        this.valueVariable = extractValueVariableName(pattern);
    }

    protected BinaryBase(BinaryBase a) {
        super(a);
        this.valueVariable = a.getValueVariable();
    }

    protected abstract VarName extractValueVariableName(VarAdmin var);
    protected abstract boolean predicatesEquivalent(BinaryBase atom);

    public VarName getValueVariable() {
        return valueVariable;
    }
    protected void setValueVariable(VarName var) {
        valueVariable = var;
    }

    @Override
    public boolean isBinary() { return true;}

    @Override
    protected boolean isRuleApplicable(InferenceRule child) {
        //TODO rule applicability for types should be disabled
        Atom ruleAtom = child.getHead().getAtom();
        return (ruleAtom instanceof BinaryBase) && this.getType().equals(ruleAtom.getType());
    }

    @Override
    public boolean requiresMaterialisation(){
        return isUserDefinedName() && getType() != null && getType().isRelationType();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = hashCode * 37 + (this.typeId != null? this.typeId.hashCode() : 0);
        hashCode = hashCode * 37 + this.varName.hashCode();
        hashCode = hashCode * 37 + this.valueVariable.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        BinaryBase a2 = (BinaryBase) obj;
        return Objects.equals(this.typeId, a2.getTypeId())
                && this.varName.equals(a2.getVarName())
                && this.valueVariable.equals(a2.getValueVariable());
    }

    @Override
    public boolean isEquivalent(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        BinaryBase a2 = (BinaryBase) obj;
        return Objects.equals(this.typeId, a2.getTypeId())
                && predicatesEquivalent(a2);
    }

    @Override
    public Set<Predicate> getIdPredicates() {
        //direct predicates
        return ((ReasonerQueryImpl) getParentQuery()).getIdPredicates().stream()
                .filter(atom -> containsVar(atom.getVarName()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Predicate> getValuePredicates(){ return new HashSet<>();}

    @Override
    public Set<Predicate> getPredicates() {
        Set<Predicate> predicates = getValuePredicates();
        predicates.addAll(getIdPredicates());
        return predicates;
    }

    /**
     * @return set of atoms that are (potentially indirectly) linked to this atom via valueVariable
     */
    public Set<Atom> getLinkedAtoms(){
        Set<Atom> atoms = new HashSet<>();
        getParentQuery().getAtoms().stream()
                .filter(Atomic::isAtom).map(atom -> (Atom) atom)
                .filter(Atom::isBinary).map(atom -> (BinaryBase) atom)
                .filter(atom -> atom.getVarName().equals(valueVariable))
                .forEach(atom -> {
                    atoms.add(atom);
                    atoms.addAll(atom.getLinkedAtoms());
                });
        return atoms;
    }

    @Override
    public Set<VarName> getVarNames() {
        Set<VarName> vars = new HashSet<>();
        if (isUserDefinedName()) vars.add(getVarName());
        if (!valueVariable.getValue().isEmpty()) vars.add(valueVariable);
        return vars;
    }

    @Override
    public Set<VarName> getSelectedNames(){
        Set<VarName> vars = super.getSelectedNames();
        if(isUserDefinedName()) vars.add(getVarName());
        if(isValueUserDefinedName()) vars.add(getValueVariable());
        return vars;
    }

    @Override
    public void unify (Map<VarName, VarName> unifiers) {
        super.unify(unifiers);
        VarName var = valueVariable;
        if (unifiers.containsKey(var)) {
            setValueVariable(unifiers.get(var));
        } else if (unifiers.containsValue(var)) {
            setValueVariable(capture(var));
        }
    }

    @Override
    public Map<VarName, VarName> getUnifiers(Atomic parentAtom) {
        if (!(parentAtom instanceof BinaryBase)) {
            throw new IllegalArgumentException(ErrorMessage.UNIFICATION_ATOM_INCOMPATIBILITY.getMessage());
        }

        Map<VarName, VarName> unifiers = new HashMap<>();
        VarName childValVarName = this.getValueVariable();
        VarName parentValVarName = ((BinaryBase) parentAtom).getValueVariable();

        if (parentAtom.isUserDefinedName()){
            VarName childVarName = this.getVarName();
            VarName parentVarName = parentAtom.getVarName();
            if (!childVarName.equals(parentVarName)) {
                unifiers.put(childVarName, parentVarName);
            }
        }
        if (!parentValVarName.getValue().isEmpty()
                && !childValVarName.equals(parentValVarName)) {
            unifiers.put(childValVarName, parentValVarName);
        }
        return unifiers;
    }
}

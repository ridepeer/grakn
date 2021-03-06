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

package ai.grakn.factory;

import ai.grakn.Grakn;
import ai.grakn.util.ErrorMessage;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class FactoryBuilderTest {
    private final static String TEST_CONFIG = "../conf/test/tinker/grakn-tinker.properties";
    private final static String KEYSPACE = "keyspace";
    private final static String ENGINE_URL = Grakn.IN_MEMORY;
    private final static Properties TEST_PROPERTIES = new Properties();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setupProperties(){
        try (InputStream in = new FileInputStream(TEST_CONFIG)){
            TEST_PROPERTIES.load(in);
        } catch (IOException e) {
            throw new RuntimeException(ErrorMessage.INVALID_PATH_TO_CONFIG.getMessage(TEST_CONFIG), e);
        }
    }

    @Test(expected=InvocationTargetException.class)
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<FactoryBuilder> c = FactoryBuilder.class.getDeclaredConstructor();
        c.setAccessible(true);
        c.newInstance();
    }

    @Test
    public void testBuildGraknFactory(){
        InternalFactory mgf = FactoryBuilder.getFactory(KEYSPACE, ENGINE_URL, TEST_PROPERTIES);
        assertThat(mgf, instanceOf(TinkerInternalFactory.class));
    }

    @Test
    public void testSingleton(){
        InternalFactory mgf1 = FactoryBuilder.getFactory(KEYSPACE, ENGINE_URL, TEST_PROPERTIES);
        InternalFactory mgf2 = FactoryBuilder.getFactory(KEYSPACE, ENGINE_URL, TEST_PROPERTIES);
        InternalFactory mgf3 = FactoryBuilder.getFactory("key", ENGINE_URL, TEST_PROPERTIES);
        InternalFactory mgf4 = FactoryBuilder.getFactory("key", ENGINE_URL, TEST_PROPERTIES);

        assertEquals(mgf1, mgf2);
        assertEquals(mgf3, mgf4);
        assertNotEquals(mgf1, mgf3);
        assertEquals(mgf1.getGraph(true), mgf2.getGraph(true));
        assertNotEquals(mgf1.getGraph(true), mgf3.getGraph(true));
    }
}
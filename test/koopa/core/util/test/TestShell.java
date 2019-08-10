package koopa.core.util.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class TestShell {

    @TestFactory
    @DisplayName("tests")
    @SuppressWarnings("unchecked")
    Iterator<? extends DynamicNode> dynamicNodes() {
        var rtc = runnableTestCases();
        if (rtc != null) return dynamicContainers(rtc);
        var rt = runnableTests();
        if (rt != null) return dynamicTests(rt);
        var cn = testClassNames();
        if (cn != null && cn.length > 0) return testSuite(cn);
        return Collections.EMPTY_LIST.iterator();
    }

    protected Iterator<Entry<String,Iterator<Entry<String,Runnable>>>> runnableTestCases()  {
        return null;
    }

    Iterator<DynamicNode> dynamicContainers(Iterator<Entry<String, Iterator<Entry<String, Runnable>>>> rtc) {
        return new Iterator<DynamicNode>() {
            @Override
            public boolean hasNext() {
                return rtc.hasNext();
            }
            @Override
            public DynamicNode next() {
                if (hasNext()) {
                    var tc = rtc.next();
                    return DynamicContainer.dynamicContainer( tc.getKey(), testSource(tc.getValue()) );
                }
                throw new NoSuchElementException();
            }
        };
    }

    @SuppressWarnings("unchecked")
    Iterable<DynamicNode> testSource(Object o) {
        if (o instanceof Iterable) {
            return (Iterable<DynamicNode>) o;
        } else {
            return () -> (Iterator<DynamicNode>) o;
        }
    }

    protected Iterator<Entry<String,Runnable>> runnableTests() {
        return null;
    }

    Iterator<DynamicNode> dynamicTests(Iterator<Entry<String, Runnable>> rt) {
        var tc = testCallbacks(this);
        return new Iterator<DynamicNode>() {
            boolean inShell = !inJupiter();
            boolean middle = false;

            @Override
            public boolean hasNext() {
                if (rt.hasNext()) {
                    return true;
                } else {
                    if (inShell) {
                        call(tc.ref,tc.afterEach);
                        inShell = false;
                    }
                    return false;
                }
            }
            @Override
            public DynamicNode next() {
                if (middle) {
                    call(tc.ref,tc.afterEach);
                }
                if (hasNext()) {
                    if (middle || inShell) {
                        call(tc.ref,tc.beforeEach);
                    }
                    middle = true;
                    var t = rt.next();
                    return DynamicTest.dynamicTest( t.getKey(), t.getValue()::run );
                }
                throw new NoSuchElementException();
            }
        };
    }

    static boolean inJupiter() {
        return StackWalker.getInstance().walk(s -> s.limit(10).anyMatch(f -> f.getClassName().startsWith("org.junit.jupiter.")) );
    }

    protected String[] testClassNames() {
        return null;
    }

    Iterator<DynamicContainer> testSuite(String[] classNames) {
        return new Iterator<DynamicContainer>() {
            int i = 0;
            Case next;

            @Override
            public boolean hasNext() {
                while (next == null && i < classNames.length) {
                    var tc = testCase(classNames[i++]);
                    if (tc != null) {
                        if (testSteps(tc)) {
                            next = tc;
                        }
                    }
                }
                return next != null;
            }
            @Override
            public DynamicContainer next() {
                if (hasNext()) {
                    var tc = next;
                    next = null;
                    var name = tc.ref.getClass().getSimpleName();
                    var tests = testCase(tc);
                    return DynamicContainer.dynamicContainer(name,tests);
                }
                throw new NoSuchElementException();
            }
        };
    }

    Iterable<DynamicNode> testCase(Case tc) {
        call(tc.ref,tc.beforeAll);
        return () -> new Iterator<DynamicNode>() {
            Method next;

            @Override
            public boolean hasNext() {
                while (next == null && !tc.test.isEmpty()) {
                    next = tc.test.remove(0);
                }
                if (next != null) {
                    return true;
                } else {
                    call(tc.ref,tc.afterAll);
                    return false;
                }
            }
            @Override
            public DynamicNode next() {
                if (hasNext()) {
                    var test = next;
                    next = null;
                    return test.isAnnotationPresent(TestFactory.class)
                        ? DynamicContainer.dynamicContainer( "tests", testSource(call(tc.ref,test)) )
                        : DynamicTest.dynamicTest(test.getName(), () -> runTest(tc.ref,tc.beforeEach,test,tc.afterEach) );
                }
                throw new NoSuchElementException();
            }
        };
    }

    static void runTest(Object ref, Method... steps) {
        for (var m:steps) {
            if (m != null) {
                try {
                    m.setAccessible(true);
                    m.invoke(ref);
                }
                catch (IllegalAccessException | IllegalArgumentException e) { uncheck(e); }
                catch (InvocationTargetException e) { uncheck(e.getCause()); }
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T call(Object ref, Method m) {
        if (m != null) {
            try {
                if (Modifier.isStatic(m.getModifiers())) {
                    m.setAccessible(true);
                    return (T) m.invoke(null);
                } else {
                    if (ref != null) {
                        m.setAccessible(true);
                        return (T) m.invoke(ref);
                    }
                }
            }
            catch (IllegalAccessException e) { uncheck(e); }
            catch (InvocationTargetException e) { uncheck(e.getCause()); }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void uncheck(Throwable t) throws T { throw (T)t; }

    static class Case {
        Object ref;
        List<Method> test;
        Method beforeAll, afterAll, beforeEach, afterEach;
    }

    static Case testCase(String className) {
        try {
            var tc = new Case();
            var type = Class.forName(className);
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            tc.ref = ctor.newInstance();
            return tc;
        }
        catch (Exception e) {
            System.err.println("could not create TestCase for "+className+": "+e);
            return null;
        }
    }

    static Case testCallbacks(Object ref) {
        var tc = new Case();
        tc.ref = ref;
        for (var c = tc.ref.getClass(); c != Object.class; c = c.getSuperclass()) {
            for (var m : c.getDeclaredMethods()) {
                if (m.getParameterCount() == 0) {
                    if (m.isAnnotationPresent(BeforeEach.class)) {
                        if (tc.beforeEach == null) tc.beforeEach = m;
                    } else if (m.isAnnotationPresent(AfterEach.class)) {
                        if (tc.afterEach == null) tc.afterEach = m;
                    }
                }
            }
        }
        return tc;
    }

    boolean testSteps(Case tc) {
        tc.test = new LinkedList<>();
        for (var c = tc.ref.getClass(); c != Object.class; c = c.getSuperclass()) {
            for (var m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (m.isAnnotationPresent(Test.class)) {
                    tc.test.add(m);
                } else if (m.isAnnotationPresent(TestFactory.class)) {
                   tc.test.add(m);
                } else if (m.isAnnotationPresent(BeforeAll.class)) {
                    if (tc.beforeAll == null) tc.beforeAll = m;
                } else if (m.isAnnotationPresent(AfterAll.class)) {
                    if (tc.afterAll == null) tc.afterAll = m;
                } else if (m.isAnnotationPresent(BeforeEach.class)) {
                    if (tc.beforeEach == null) tc.beforeEach = m;
                } else if (m.isAnnotationPresent(AfterEach.class)) {
                    if (tc.afterEach == null) tc.afterEach = m;
                }
            }
        }
        return !tc.test.isEmpty();
    }

}

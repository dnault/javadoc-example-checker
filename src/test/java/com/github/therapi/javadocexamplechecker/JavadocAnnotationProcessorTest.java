package com.github.therapi.javadocexamplechecker;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.assertEquals;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

public class JavadocAnnotationProcessorTest {

    private static final String DOCUMENTED_CLASS = "javasource.foo.DocumentedClass";
    private static final String ANOTHER_DOCUMENTED_CLASS = "javasource.bar.AnotherDocumentedClass";
    private static final String ANNOTATED_WITH_RETAIN_JAVADOC = "javasource.bar.YetAnotherDocumentedClass";
    private static final String UNDOCUMENTED = "javasource.bar.UndocumentedClass";
    private static final String BLANK_COMMENTS = "javasource.bar.BlankDocumentation";
    private static final String METHOD_DOC_BUT_NO_CLASS_DOC = "javasource.bar.OnlyMethodDocumented";

    private static List<JavaFileObject> sources() {
        List<JavaFileObject> files = new ArrayList<>();
        for (String resource : new String[]{
                "javasource/bar/DocumentedClass.java",
        }) {
            files.add(JavaFileObjects.forResource(resource));
        }
        return files;
    }

    private static CompilationClassLoader compile(String options) {
        com.google.testing.compile.Compiler compiler = javac()
                .withProcessors(new JavadocExampleCheckerAnnotationProcessor());

        if (options != null) {
            compiler = compiler.withOptions(options);
        }

        Compilation compilation = compiler.compile(sources());
        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningCount(0);

        return new CompilationClassLoader(compilation);
    }

    @Test
    public void companionGeneratedForClassWithMethodDocButNoClassDoc() throws Exception {
        try (CompilationClassLoader classLoader = compile(null)) {

        }
    }
}

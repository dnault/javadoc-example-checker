package com.github.therapi.javadocexamplechecker;

import com.google.common.base.Joiner;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.testing.compile.Compiler.javac;

public class JavadocExampleCheckerAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        final Elements elements = processingEnv.getElementUtils();

        // Make sure each element only gets processed once.
        final Set<Element> alreadyProcessed = new HashSet<>();

        for (Element e : roundEnvironment.getRootElements()) {
            processJavadocRecursive(elements, e, alreadyProcessed);
        }

        return false;
    }

    private void processJavadocRecursive(Elements elements, Element e, Set<Element> alreadyProcessed) {
        if (!alreadyProcessed.add(e)) {
            return;
        }

        String javadoc = elements.getDocComment(e);
        if (!isBlank(javadoc)) {
            try {
                processJavadoc(javadoc, e);
            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Javadoc retention failed; " + ex, e);
                throw new RuntimeException("Javadoc example check failed for " + e, ex);
            }
        }

        for (Element enclosed : e.getEnclosedElements()) {
            processJavadocRecursive(elements, enclosed, alreadyProcessed);
        }
    }

    private static final Pattern exampleCodePattern = Pattern.compile("<pre><code>(.+)</code></pre>", Pattern.DOTALL);

    private static String getClasspathFromClassloader(ClassLoader currentClassloader) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        // Add all URLClassloaders in the hirearchy till the system classloader.
        List<URLClassLoader> classloaders = new ArrayList<>();
        while (currentClassloader != null) {
            if (currentClassloader instanceof URLClassLoader) {
                // We only know how to extract classpaths from URLClassloaders.
                classloaders.add((URLClassLoader) currentClassloader);
            } else {
                System.out.println("WARN: " + currentClassloader + " is not URLCLassloader");
//                throw new IllegalArgumentException("Classpath for compilation could not be extracted "
//                        + "since given classloader is not an instance of URLClassloader");
            }
            if (currentClassloader == systemClassLoader) {
                break;
            }
            currentClassloader = currentClassloader.getParent();
        }

        Set<String> classpaths = new LinkedHashSet<>();

        // fixme Need to not hard-code this
        classpaths.add("src/main/java");

        for (URLClassLoader classLoader : classloaders) {
            for (URL url : classLoader.getURLs()) {
                if (url.getProtocol().equals("file")) {
                    classpaths.add(url.getPath());
                } else {
                    throw new IllegalArgumentException("Given classloader consists of classpaths which are "
                            + "unsupported for compilation.");
                }
            }
        }

        return Joiner.on(':').join(classpaths);
    }

    private void processJavadoc(String javadoc, Element e) throws IOException {
        final Matcher m = exampleCodePattern.matcher(javadoc);
        while (m.find()) {
            String exampleCode = m.group(1);
            exampleCode = exampleCode.replaceAll("(?m)^(\\s)?#", "");
            System.out.println(exampleCode);

            JavaFileObject obj = JavaFileObjects.forSourceString("JavadocExample", exampleCode);

            System.out.println(getClass().getClassLoader());
            System.out.println(getClass().getClassLoader().getClass());

            String classpath = getClasspathFromClassloader(getClass().getClassLoader());
            System.out.println("CLASSPATH: " + classpath);

            com.google.testing.compile.Compiler compiler = javac().withOptions("-classpath", classpath);
            Compilation compilation = compiler.compile(obj);
            System.out.println(compilation.generatedFiles());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }
}

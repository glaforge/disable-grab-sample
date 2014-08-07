Disabling @Grab with a global AST transformation
================================================

On the Groovy mailing-list, we had an interesting question about how to disable annotations like @Grab, to prevent users from downloading third-party dependencies.
There are a few possibilities for that, but my favorite was to create a [global AST transformation](http://groovy.codehaus.org/Global+AST+Transformations) that would generate a compilation error if the `@Grab` annotation is found on an import.

I created a first [small prototype](http://groovyconsole.appspot.com/script/5686306919153664) within a script, but I used an injected local transformation to get everything working with a simple script. So I decided afterwards to do it for real this time, using a real project on Github with a proper global AST transformation this time.

If you checkout the project from Github, with:

    git clone git@github.com:glaforge/disable-grab-sample.git

You then `cd` in the `disable-grab-sample` directory, and you can run the following command to launch the Spock test showing the transformation in action:

    ./gradlew test
    
So what's inside that project? First of all, we need to create our AST transformation (I'll skip the imports for brevity):

```groovy
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class DisableGrabTransformation implements ASTTransformation {
    void visit(ASTNode[] nodes, SourceUnit source) {
        def imports = source.AST.imports
        
        if (imports) {
            imports.each { anImport ->
                anImport.annotations.each { anno ->
                    if (anno.classNode.name in ['groovy.lang.Grab', 'groovy.lang.Grapes']) {
                        source.errorCollector.addError(
                            new SyntaxErrorMessage(
                                new SyntaxException('@Grab and @Grapes are forbidden',
                                anImport.lineNumber, anImport.columnNumber), 
                            source))
                    }
                }
            }
        }
    }
}
```

We create a class implementing the `ASTTransformation` class with its `visit()` method. In that method, we access the "module" imports, and if there are any import, we iterate over each of them, checking if there are annotations put on them. If those imports are annotated with `@Grab` or `@Grapes` (ie. if the annotation class node's fully qualified class name are the FQN of the Grab and Grapes annotations), we then use the error collector to add a new syntax error, so that a compilation error is thrown by the Groovy compiler if ever someone uses @Grab in a Groovy script or class.

We need to wire in that global transformation. As they are not triggered by annotations like local transformations, we need do declare the transformation in a specific file: 

    META-INF/services/org.codehaus.groovy.transform.ASTTransformation
    
This file will just contain one line: the fully qualified class name of the AST transformation that needs to be applied to each script and classes that will be compiled when this transformation is on the classpath. So our services file will just contain:

    disablegrab.DisableGrabTransformation
    
Now we need to see if our transformation is applied, and works as expected. For that purpose, we'll create a [Spock](http://spock-framework.readthedocs.org/en/latest/) test:

```groovy
class DisableGrabSpec extends Specification {
    def "test"() {
        given:
        def shell = new GroovyShell()
        when:
        shell.evaluate '''
            @Grab('org.apache.commons:commons-lang3:3.3.2')
            import org.apache.commons.lang3.StringUtils
            println "hi"        
        '''
        then:
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains('@Grab and @Grapes are forbidden')
    }
}
```

We are using the `GroovyShell` class to evaluate (and thus compile) a script that contains a `@Grab` instruction. When evaluating that script, a compilation error should be thrown, and we indeed assert that it's the case, and that our compilation error contains the message we've crafted.

Done!

Let's step back a little with a couple words about our build file:

```groovy
apply plugin: 'groovy'

repositories {
    jcenter()
}

dependencies {
    compile 'org.codehaus.groovy:groovy-all:2.3.6'
    testCompile 'org.spockframework:spock-core:0.7-groovy-2.0'
    testCompile 'org.apache.ivy:ivy:2.4.0-rc1'
}
```

Not much to see here actually! We're just applying the groovy plugin, use the jcenter repository from Bintray. We're using `groovy-all`, the Spock library for our test scope, as well as the Ivy library that's needed by the grape infrastructure for fully functioning, for retrieving artifacts.

With our Gradle build file, we can call the jar task to create a JAR that will contain the META-INF/services file, and as soon as you'll have that JAR on your classpath with that AST transformation, any script or class compiled with it will get compilation errors if `@Grab` is used.

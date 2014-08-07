package disablegrab

import org.codehaus.groovy.transform.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.control.customizers.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.messages.*
import org.codehaus.groovy.syntax.*

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

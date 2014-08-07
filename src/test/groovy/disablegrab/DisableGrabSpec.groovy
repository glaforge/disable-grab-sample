package disablegrab

import spock.lang.Specification
import org.codehaus.groovy.control.*

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

// verify.groovy
// Integration test for the SysML Mission Generator Maven plugin.

def generatedDir = new File(
    basedir,
    "target/generated-sources/sysml"
)

assert generatedDir.isDirectory() :
    "SysML generator did not create the output directory"

def javaFiles = []
generatedDir.eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        javaFiles << file
    }
}

assert !javaFiles.isEmpty() :
    "SysML generator did not produce any Java source"

// Verify that generated Java sources have been compiled.
def classesDir = new File(
    basedir,
    "target/classes"
)

assert classesDir.isDirectory() :
    "Maven did not create the classes directory"

def classFiles = []
classesDir.eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".class")) {
        classFiles << file
    }
}

assert !classFiles.isEmpty() :
    "Generated Java sources were not compiled"

println "SysML generation successful: ${javaFiles.size()} Java files"
println "Java compilation successful: ${classFiles.size()} class files"

return true
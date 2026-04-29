package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.*;
import fr.inria.mbdo.mission.ir.*;
import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.util.*;

import static fr.inria.mbdo.mission.utils.StringUtils.toUpperFirst;

public class JavaTransformer {

    private final Logger logger = LoggerFactory.getLogger(JavaTransformer.class);

    private final IrRepository irRepository;
    private final TypeTable typeTable;
    private final String packagePrefix;

    public JavaTransformer(IrRepository irRepository, TypeTable typeTable, String packagePrefix) {
        this.irRepository = irRepository;
        this.typeTable = typeTable;
        this.packagePrefix = packagePrefix;
    }

    public Map<String, JavaFile> generate() {

        Map<String, JavaFile> result = new HashMap<>();

        result.putAll(Objects.requireNonNull(generateMachinesInterfaces()));
        result.putAll(Objects.requireNonNull(generateEnumerations()));
        result.putAll(Objects.requireNonNull(generateMachinesMissionsClasses()));

        return result;
    }

    private Map<String, JavaFile> generateMachinesInterfaces() {
        Map<String, JavaFile> javaFiles = new HashMap<>();

        for (Map.Entry<String, MachineIR> entry : irRepository.getMachines().entrySet()) {
            logger.info("Generating machine interface for {}", entry.getKey());

            MachineIR machine = entry.getValue();
            TypeSpec.Builder partDefInterfaceSpecBuilder = TypeSpec.interfaceBuilder(machine.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("From $L\n$L", machine.getQualifiedName(), machine.getDocumentation());

            List<MethodSpec> methods = new ArrayList<>();

            for (MachineAttributeIR attribute : machine.getAttributes()) {
                MethodSpec.Builder getterBuilder = MethodSpec
                        .methodBuilder("get" + toUpperFirst(attribute.getName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(typeTable.resolve(attribute.getType()));

                MethodSpec.Builder setterBuilder = MethodSpec
                        .methodBuilder("set" + toUpperFirst(attribute.getName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(typeTable.resolve(attribute.getType()), attribute.getName());

                methods.add(getterBuilder.build());
                methods.add(setterBuilder.build());
            }

            for (ActionIR action : machine.getActions()) {
                MethodSpec.Builder actionBuilder = MethodSpec
                        .methodBuilder(action.getName())
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameters(action.getParameters().stream().map(
                                p -> ParameterSpec.builder(typeTable.resolve(p.getType()), p.getName()).build())
                                .toList())
                        .addJavadoc("From $L\n$L", action.getQualifiedName(), action.getDocumentation());

                methods.add(actionBuilder.build());
            }

            partDefInterfaceSpecBuilder.addMethods(methods);

            JavaFile javaFile = JavaFile.builder(getJavaPackageFor(machine), partDefInterfaceSpecBuilder.build())
                    .build();
            javaFiles.put(machine.getQualifiedName(), javaFile);
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateEnumerations() {
        Map<String, JavaFile> javaFiles = new HashMap<>();

        for (Map.Entry<String, EnumerationIR> entry : irRepository.getEnumerations().entrySet()) {
            logger.info("Generating enumeration for {}", entry.getKey());

            EnumerationIR enumeration = entry.getValue();
            TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumeration.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("From $L\n$L", enumeration.getQualifiedName(), enumeration.getDocumentation());

            for (String constant : enumeration.getConstants()) {
                enumBuilder.addEnumConstant(constant);
            }

            JavaFile javaFile = JavaFile.builder(getJavaPackageFor(enumeration), enumBuilder.build()).build();
            javaFiles.put(enumeration.getQualifiedName(), javaFile);
        }

        return javaFiles;
    }

    private Map<String, JavaFile> generateMachinesMissionsClasses() {
        Map<String, JavaFile> javaFiles = new HashMap<>();

        for (Map.Entry<String, MachineMissionIR> entry : irRepository.getMissions().entrySet()) {
            logger.info("Generating mission for {}", entry.getKey());

            MachineMissionIR mission = entry.getValue();
            TypeSpec.Builder missionBuilder = TypeSpec.classBuilder(mission.getName())
                    .addJavadoc("From $L\n$L", mission.getQualifiedName(), mission.getDocumentation());

            List<MethodSpec> methods = new ArrayList<>();
            List<FieldSpec> fields = new ArrayList<>();

            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC);

            for (MachineRefIR machine : mission.getMachinesRefs()) {
                String fieldName = machine.name();
                TypeName fieldType = typeTable.resolve(machine.type());

                fields.add(FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                constructorBuilder.addParameter(fieldType, fieldName);
                constructorBuilder.addStatement("this.$N = $N", fieldName, fieldName);
            }

            for (var state : mission.getStates()) {
                // TODO
            }

            // Custom actions are emitted from mission IR once populated.

            missionBuilder.addSuperinterface(ParameterizedTypeName.get(MachineMissionStrategy.class))
                    .addFields(fields)
                    .addMethods(methods)
                    .addMethod(constructorBuilder.build());

            JavaFile javaFile = JavaFile.builder(getJavaPackageFor(mission), missionBuilder.build()).build();
            javaFiles.put(mission.getQualifiedName(), javaFile);
        }

        return javaFiles;
    }

    /**
     * Converts SysML type to Javapoet TypeName
     * 
     * @param type String SysML type
     * @return the TypeName corresponding to input SysML type
     */
    private String getJavaPackageFor(ElementIR element) {
        return packagePrefix + "." + element.getJavaPackage();
    }
}

package injecter.processor;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import injecter.annotation.Message;
import injecter.annotation.TransferModule;

/**
 * 注解处理器
 */
@AutoService(Processor.class)
public final class InjecterProcessor extends AbstractProcessor{

    private Elements elementUtils;
    private Filer filer; //生成源代码
    private Messager messager; //打印信息
    private static final ClassName VIEW_BINDER = ClassName.get("injecter.api", "ViewBinder");//实现的接口

    private static final String CLASS_NAME_TYPE = "MessageType";//生成type类

    private static final String CLASS_NAME_UTIL_SUFFIX = "MessageUtil";//生成type类

    private static final String CLASS_PACKAGE="com.annotation";

    Map<String, List<MessageTypeBinding>> messageMap = new LinkedHashMap<>();


    @Override public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        filer = env.getFiler();
        messager = env.getMessager();
    }

    /**
     * 需要处理的注解
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Message.class.getCanonicalName());
        return types;
    }

    /**
     * 支持的源码版本
     * @return
     */
    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void log(String... logs){
        int size = logs.length;
        String str="";
        for (int i = 0; i < size; i++) {
            str += logs[i]+" ";
        }
        messager.printMessage(Diagnostic.Kind.WARNING, str);
    }

    /**
     * 获取注解信息，动态生成代码
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messageMap = new LinkedHashMap<>();
        Set<? extends ExecutableElement> elementsAnnotateds= (Set<? extends ExecutableElement>) roundEnv.getElementsAnnotatedWith(Message.class);
        parseData(elementsAnnotateds);
        generateMessageType();
        generateMessageUtil();
        return true;
    }

    //解析所有以Message注解的信息
    private void parseData(Set<? extends ExecutableElement> data){
        // 将所有Message注解以module为key，将注解中的数据放到MessageTypeBinding数组中存起来
        for (ExecutableElement element:data){
            log(element.toString());

            if (!SuperficialValidation.validateElement(element))
                continue;
            String module = element.getAnnotation(Message.class).module();

//            boolean hasError = isInaccessibleViaGeneratedCode(Message.class, "fields", element)
//                        || isBindingInWrongPackage(Message.class, element);
            List<MessageTypeBinding> messageTypeBindingList = messageMap.get(module);

            if (messageTypeBindingList == null) {
                messageTypeBindingList = new ArrayList<>();
                messageMap.put(module,messageTypeBindingList);
            }
            TransferModule[] trans = element.getAnnotation(Message.class).trans();
            String name = element.getSimpleName().toString();
            String classname = element.getEnclosingElement().toString();
            boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
            List<? extends VariableElement> parameters = ((ExecutableElement) element).getParameters();
            int size = parameters.size();
            TypeName[] params=new TypeName[size];

            for (int i = 0; i < size; i++) {
                params[i]=TypeName.get(parameters.get(i).asType());
            }

            MessageTypeBinding messageTypeBinding = new MessageTypeBinding(classname,name, module, trans, params,isStatic);
            messageTypeBindingList.add(messageTypeBinding);
        }
    }

    //生成MessageType文件
    private void generateMessageType(){
        //构造Java类名
        TypeSpec.Builder messageType = TypeSpec.classBuilder(CLASS_NAME_TYPE)
                .addModifiers(Modifier.PUBLIC) ;
        int index=0;

        //构造BASE_MODULE对应的值
        for (Map.Entry<String, List<MessageTypeBinding>> item : messageMap.entrySet()){
            List<MessageTypeBinding> list = item.getValue();
            if (list == null || list.size() == 0){
                continue;
            }
            String module = item.getKey();
            messageType.addField(createBaseField(module,index));
            index++;
        }
        index=0;

        //构造各个moudle下MESSAGE_TYPE对应的值
        for (Map.Entry<String, List<MessageTypeBinding>> item : messageMap.entrySet()){
            List<MessageTypeBinding> list = item.getValue();
            if (list == null || list.size() == 0){
                continue;
            }
            String module = item.getKey();
            int size = list.size();
            MessageTypeBinding binding;
            for (int i = 0; i < size; i++) {
                binding = list.get(i);
                messageType.addField(createTypeField(module,binding.getMethodName(),index,i));
            }
            index++;
        }
        try {
            //生成Java文件
            JavaFile.builder(CLASS_PACKAGE, messageType.build())
                    .addFileComment(" This codes are generated automatically. Do not modify!")
                    .build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //构造BASE_MODULE对应的值
    private FieldSpec createBaseField(String module, int index) {
        int in=index << 24;
        String s;
        if(index<0x10){
            s=String.format("0x0%x",in);
        }else {
            s=String.format("0x%x",in);
        }
        FieldSpec.Builder result = FieldSpec.builder(TypeName.INT,"TYPE_"+module.toUpperCase(),
                Modifier.PRIVATE,Modifier.FINAL,Modifier.STATIC)
                .initializer(CodeBlock.of(s));

        return result.build();
    }

    //构造各个moudle下MESSAGE_TYPE对应的值
    private FieldSpec createTypeField(String module,String name,int baseindex,int index) {

        log("module=" +module+
                "name=" +name+
                "baseindex=" +baseindex+
                "index=" +index);

        String s;
        int in= (baseindex << 24) +index;
        if(baseindex<0x10){
            s=String.format("0x0%x",in);
        }else {
            s=String.format("0x%x",in);
        }
        FieldSpec.Builder result = FieldSpec.builder(TypeName.INT,"MESSAGE_"+module.toUpperCase()+"_"+name.toUpperCase(),
                Modifier.PUBLIC,Modifier.FINAL,Modifier.STATIC)
                .initializer(CodeBlock.of(s));

        if(index==0){
            result.addJavadoc("type $S",module);
        }

        return result.build();
    }

    private String getMessageTypeName(String module,String name){
        return "MESSAGE_"+module.toUpperCase()+"_"+name.toUpperCase();
    }


    //生成<MODUEL>MessageUtil文件,比如LoginMessageUtil
    private void generateMessageUtil(){

        for (Map.Entry<String, List<MessageTypeBinding>> item : messageMap.entrySet()){
            List<MessageTypeBinding> list = item.getValue();
            if (list == null || list.size() == 0){
                continue;
            }
            String module = item.getKey();

            //构造Java类名
            TypeSpec.Builder messageType = TypeSpec.classBuilder(module+CLASS_NAME_UTIL_SUFFIX)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(ClassName.get(CLASS_PACKAGE, "MessageUtil.BaseMessageHandler"))
                    ;

            int size = list.size();
            MessageTypeBinding binding;

            MethodSpec.Builder result = MethodSpec.methodBuilder("handleMessage")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.VOID)
                    .addParameter(TypeName.get(int.class), "type")
                    .addParameter(TypeName.get(Object[].class), "data")
                    .varargs();
            result.addCode("    switch (type) {\n");

            for (int i = 0; i < size; i++) {
                binding = list.get(i);
                createBindMethod(result,binding);
            }

            result.addCode(
                    "        default:\n" +
                    "            break;\n" +
                    "    }\n");

            messageType.addMethod(result.build());

            try {
                //生成Java文件
                JavaFile.builder(CLASS_PACKAGE, messageType.build())
                        .addFileComment(" This codes are generated automatically. Do not modify!")
                        .build().writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private void createBindMethod(MethodSpec.Builder result,MessageTypeBinding binding) {
        ClassName className = ClassName.get(CLASS_PACKAGE,"MessageType");
        result.addCode("        case ");
        result.addCode(CodeBlock.of("$T",className));
        result.addCode("."+getMessageTypeName(binding.getModule(),binding.getMethodName())+" :\n");
        int size = binding.getParams().length;
        result.addCode("if(data.length==" + binding.getParams().length);
        TypeName typeName;
        for (int i = 0; i < size; i++) {
            typeName = binding.getParams()[i];
            result.addCode("\n&&data[$L] instanceof $T", i, typeName);
        }
        result.addCode("){\n");
        if (binding.isStatic()) {
            result.addCode("$T." + binding.getMethodName() + "(", ClassName.bestGuess(binding.getClassName()));
        } else {
            result.addCode("new $T()." + binding.getMethodName() + "(", ClassName.bestGuess(binding.getClassName()));
        }
        for (int i = 0; i < size; i++) {
            typeName = binding.getParams()[i];
            if (i > 0) {
                result.addCode(",");
            }
            result.addCode("($T", typeName);
            result.addCode(")data[$L]", i);
        }
        result.addCode(");\n");
        result.addCode("}");

        result.addCode("            break;\n");
    }


    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        int x=0;
        if(x==0){
            return false;
        }

        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName()));
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != ElementKind.CLASS) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName()));
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(Modifier.PRIVATE)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName()));
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName));
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName));
            return true;
        }

        return false;
    }

    private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (otherType.equals(typeMirror.toString())) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInterface(TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType
                && ((DeclaredType) typeMirror).asElement().getKind() == ElementKind.INTERFACE;
    }

    private String getPackageName(TypeElement type) {

        String s=elementUtils.getPackageOf(type).getQualifiedName().toString();

        messager.printMessage(Diagnostic.Kind.WARNING, "Printing: " + s);
        return s;

    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }
}
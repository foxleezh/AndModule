package injecter.processor;


import com.squareup.javapoet.TypeName;

import injecter.annotation.TransferModule;

/**
 * Created by foxleezh on 18-3-7.
 */
final class MessageTypeBinding {
    private final String className;
    private final String methodName;
    private final String module;
    private final TransferModule[] trans;
    private final TypeName[] params;
    private boolean isStatic;

    public MessageTypeBinding(String className, String methodName, String module, TransferModule[] trans, TypeName[] params, boolean isStatic) {
        this.className = className;
        this.methodName = methodName;
        this.module = module;
        this.trans = trans;
        this.params = params;
        this.isStatic = isStatic;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getModule() {
        return module;
    }

    public TransferModule[] getTrans() {
        return trans;
    }

    public TypeName[] getParams() {
        return params;
    }

    public boolean isStatic() {
        return isStatic;
    }


}
package cn.tesseract.mycelium.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.tesseract.mycelium.asm.ReturnCondition.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class AsmHook implements Comparable<AsmHook> {
    private String targetClassName;
    private boolean customClassName;
    private String targetMethodName;
    private List<Type> targetMethodParameters = new ArrayList<Type>(2);
    private Type targetMethodReturnType;

    protected String hookClassName;
    protected String hookMethodName;
    private List<Integer> transmittableVariableIds = new ArrayList<Integer>(2);
    private List<Type> hookMethodParameters = new ArrayList<Type>(2);
    private Type hookMethodReturnType = Type.VOID_TYPE;
    private boolean hasReturnValueParameter;

    private ReturnCondition returnCondition = ReturnCondition.NEVER;
    private ReturnValue returnValue = ReturnValue.VOID;
    private Object primitiveConstant;

    private HookInjectorFactory injectorFactory = HookInjectorFactory.enter;
    private String[] injectorSettings = new String[0];
    private HookPriority priority = HookPriority.NORMAL;
    private String targetMethodDescription;
    private String hookMethodDescription;
    private String returnMethodName;
    private String returnMethodDescription;

    private boolean createMethod;
    private boolean isMandatory;
    public boolean injected;

    protected String getTargetClassName() {
        return targetClassName;
    }

    public String getTargetClassInternalName() {
        return targetClassName.replace('.', '/');
    }

    public String getHookClassInternalName() {
        return hookClassName.replace('.', '/');
    }

    protected boolean isTargetMethod(String name, String desc) {
        return (targetMethodReturnType == null && desc.startsWith(targetMethodDescription) ||
                desc.equals(targetMethodDescription)) && name.equals(targetMethodName);
    }

    protected boolean getCreateMethod() {
        return createMethod;
    }

    protected boolean isMandatory() {
        return isMandatory;
    }

    protected HookInjectorFactory getInjectorFactory() {
        return injectorFactory;
    }

    protected String[] getInjectorSettings() {
        return injectorSettings;
    }

    public boolean hasHookMethod() {
        return hookMethodName != null && hookClassName != null;
    }

    protected void createMethod(HookInjectorClassVisitor classVisitor) {
        ClassMetadataReader.MethodReference superMethod = classVisitor.transformer.classMetadataReader.findVirtualMethod(getTargetClassInternalName(), targetMethodName, targetMethodDescription);
        MethodVisitor mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC,
                superMethod == null ? targetMethodName : superMethod.name, targetMethodDescription, null, null);
        if (mv instanceof HookInjector) {
            HookInjector inj = (HookInjector) mv;
            inj.visitCode();
            inj.visitLabel(new Label());
            if (superMethod == null) {
                injectDefaultValue(inj, targetMethodReturnType);
            } else {
                injectSuperCall(inj, superMethod);
            }
            injectReturn(inj, targetMethodReturnType);
            inj.visitLabel(new Label());
            inj.visitMaxs(0, 0);
            inj.visitEnd();
        } else {
            throw new IllegalArgumentException("Hook injector not created");
        }
    }

    protected void inject(HookInjector inj) {
        Type targetMethodReturnType = inj.methodType.getReturnType();

        int returnLocalId = -1;
        if (hasReturnValueParameter) {
            returnLocalId = inj.newLocal(targetMethodReturnType);
            inj.visitVarInsn(targetMethodReturnType.getOpcode(54), returnLocalId); //storeLocal
        }

        int hookResultLocalId = -1;
        if (hasHookMethod()) {
            injectInvokeStatic(inj, returnLocalId, hookMethodName, hookMethodDescription);

            if (returnValue == ReturnValue.HOOK_RETURN_VALUE || returnCondition.requiresCondition) {
                hookResultLocalId = inj.newLocal(hookMethodReturnType);
                inj.visitVarInsn(hookMethodReturnType.getOpcode(54), hookResultLocalId); //storeLocal
            }
        }

        if (returnCondition != ReturnCondition.NEVER) {
            Label label = inj.newLabel();

            if (returnCondition != ALWAYS) {
                inj.visitVarInsn(hookMethodReturnType.getOpcode(21), hookResultLocalId); //loadLocal
                if (returnCondition == ReturnCondition.ON_TRUE) {
                    inj.visitJumpInsn(IFEQ, label);
                } else if (returnCondition == ReturnCondition.ON_NULL) {
                    inj.visitJumpInsn(IFNONNULL, label);
                } else if (returnCondition == ReturnCondition.ON_NOT_NULL) {
                    inj.visitJumpInsn(IFNULL, label);
                }
            }

            if (returnValue == ReturnValue.NULL) {
                inj.visitInsn(Opcodes.ACONST_NULL);
            } else if (returnValue == ReturnValue.PRIMITIVE_CONSTANT) {
                inj.visitLdcInsn(primitiveConstant);
            } else if (returnValue == ReturnValue.HOOK_RETURN_VALUE) {
                inj.visitVarInsn(hookMethodReturnType.getOpcode(21), hookResultLocalId); //loadLocal
            } else if (returnValue == ReturnValue.ANOTHER_METHOD_RETURN_VALUE) {
                String returnMethodDescription = this.returnMethodDescription;
                if (returnMethodDescription.endsWith(")")) {
                    returnMethodDescription += targetMethodReturnType.getDescriptor();
                }
                injectInvokeStatic(inj, returnLocalId, returnMethodName, returnMethodDescription);
            }

            injectReturn(inj, targetMethodReturnType);

            inj.visitLabel(label);
        }

        if (hasReturnValueParameter) {
            injectLoad(inj, targetMethodReturnType, returnLocalId);
        }
    }

    public static void injectLoad(MethodVisitor inj, Type parameterType, int variableId) {
        int opcode;
        if (parameterType == INT_TYPE || parameterType == BYTE_TYPE || parameterType == CHAR_TYPE ||
                parameterType == BOOLEAN_TYPE || parameterType == SHORT_TYPE) {
            opcode = ILOAD;
        } else if (parameterType == LONG_TYPE) {
            opcode = LLOAD;
        } else if (parameterType == FLOAT_TYPE) {
            opcode = FLOAD;
        } else if (parameterType == DOUBLE_TYPE) {
            opcode = DLOAD;
        } else {
            opcode = ALOAD;
        }
        inj.visitVarInsn(opcode, variableId);
    }

    public void injectSuperCall(HookInjector inj, ClassMetadataReader.MethodReference method) {
        int variableId = 0;
        for (int i = 0; i <= targetMethodParameters.size(); i++) {
            Type parameterType = i == 0 ? TypeHelper.getType(targetClassName) : targetMethodParameters.get(i - 1);
            injectLoad(inj, parameterType, variableId);
            if (parameterType.getSort() == Type.DOUBLE || parameterType.getSort() == Type.LONG) {
                variableId += 2;
            } else {
                variableId++;
            }
        }
        inj.visitMethodInsn(INVOKESPECIAL, method.owner, method.name, method.desc, false);
    }

    public static void injectDefaultValue(MethodVisitor inj, Type targetMethodReturnType) {
        switch (targetMethodReturnType.getSort()) {
            case Type.VOID:
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                inj.visitInsn(Opcodes.ICONST_0);
                break;
            case Type.FLOAT:
                inj.visitInsn(Opcodes.FCONST_0);
                break;
            case Type.LONG:
                inj.visitInsn(Opcodes.LCONST_0);
                break;
            case Type.DOUBLE:
                inj.visitInsn(Opcodes.DCONST_0);
                break;
            default:
                inj.visitInsn(Opcodes.ACONST_NULL);
                break;
        }
    }

    public static void injectReturn(MethodVisitor inj, Type targetMethodReturnType) {
        if (targetMethodReturnType == INT_TYPE || targetMethodReturnType == SHORT_TYPE ||
                targetMethodReturnType == BOOLEAN_TYPE || targetMethodReturnType == BYTE_TYPE
                || targetMethodReturnType == CHAR_TYPE) {
            inj.visitInsn(IRETURN);
        } else if (targetMethodReturnType == LONG_TYPE) {
            inj.visitInsn(LRETURN);
        } else if (targetMethodReturnType == FLOAT_TYPE) {
            inj.visitInsn(FRETURN);
        } else if (targetMethodReturnType == DOUBLE_TYPE) {
            inj.visitInsn(DRETURN);
        } else if (targetMethodReturnType == VOID_TYPE) {
            inj.visitInsn(RETURN);
        } else {
            inj.visitInsn(ARETURN);
        }
    }

    public void injectInvokeStatic(HookInjector inj, int returnLocalId, String name, String desc) {
        for (int i = 0; i < hookMethodParameters.size(); i++) {
            Type parameterType = hookMethodParameters.get(i);
            int variableId = transmittableVariableIds.get(i);
            if (inj.isStatic) {
                if (variableId == 0) {
                    inj.visitInsn(Opcodes.ACONST_NULL);
                    continue;
                }
                if (variableId > 0) variableId--;
            }
            if (variableId == -1) variableId = returnLocalId;
            injectLoad(inj, parameterType, variableId);
        }

        inj.visitMethodInsn(INVOKESTATIC, getHookClassInternalName(), name, desc, false);
    }

    public String getPatchedMethodName() {
        return targetClassName + '#' + targetMethodName + targetMethodDescription;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AsmHook: ");

        sb.append(targetClassName).append('#').append(targetMethodName);
        sb.append(targetMethodDescription);
        sb.append(" -> ");
        sb.append(hookClassName).append('#').append(hookMethodName);
        sb.append(hookMethodDescription);

        sb.append(", ReturnCondition=").append(returnCondition);
        sb.append(", ReturnValue=").append(returnValue);
        if (returnValue == ReturnValue.PRIMITIVE_CONSTANT) sb.append(", Constant=").append(primitiveConstant);
        sb.append(", InjectorFactory: ").append(injectorFactory.getClass().getName());
        sb.append(", CreateMethod = ").append(createMethod);

        return sb.toString();
    }

    @Override
    public int compareTo(AsmHook o) {
        if (injectorFactory.isPriorityInverted && o.injectorFactory.isPriorityInverted) {
            return o.priority.ordinal() - priority.ordinal();
        } else if (!injectorFactory.isPriorityInverted && !o.injectorFactory.isPriorityInverted) {
            return priority.ordinal() - o.priority.ordinal();
        } else {
            return injectorFactory.isPriorityInverted ? 1 : -1;
        }
    }

    public static Builder newBuilder() {
        return new AsmHook().new Builder();
    }

    public class Builder extends AsmHook {

        public Builder() {

        }

        public Builder setTargetClass(String className) {
            AsmHook.this.targetClassName = className;
            return this;
        }

        public Builder setCustomTargetClass(String className) {
            AsmHook.this.targetClassName = className;
            AsmHook.this.customClassName = true;
            return this;
        }

        public Builder setTargetMethod(String methodName) {
            AsmHook.this.targetMethodName = methodName;
            return this;
        }

        public Builder addTargetMethodParameters(Type... parameterTypes) {
            AsmHook.this.targetMethodParameters.addAll(Arrays.asList(parameterTypes));
            return this;
        }

        public Builder addTargetMethodParameters(String... parameterTypeNames) {
            Type[] types = new Type[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                types[i] = TypeHelper.getType(parameterTypeNames[i]);
            }
            return addTargetMethodParameters(types);
        }

        public Builder setTargetMethodReturnType(Type returnType) {
            AsmHook.this.targetMethodReturnType = returnType;
            return this;
        }

        public Builder setTargetMethodReturnType(String returnType) {
            return setTargetMethodReturnType(TypeHelper.getType(returnType));
        }

        public Builder setHookClass(String className) {
            this.hookClassName = className;
            AsmHook.this.hookClassName = className;
            return this;
        }

        public Builder setHookMethod(String methodName) {
            this.hookMethodName = methodName;
            AsmHook.this.hookMethodName = methodName;
            return this;
        }

        public Builder addHookMethodParameter(Type parameterType, int variableId) {
            if (!AsmHook.this.hasHookMethod()) {
                throw new IllegalStateException("Hook method is not specified, so can not append " +
                        "parameter to its parameters list.");
            }
            AsmHook.this.hookMethodParameters.add(parameterType);
            AsmHook.this.transmittableVariableIds.add(variableId);
            return this;
        }

        public Builder addHookMethodParameter(String parameterTypeName, int variableId) {
            return addHookMethodParameter(TypeHelper.getType(parameterTypeName), variableId);
        }

        public Builder addThisToHookMethodParameters() {
            if (!AsmHook.this.hasHookMethod()) {
                throw new IllegalStateException("Hook method is not specified, so can not append " +
                        "parameter to its parameters list.");
            }
            if (AsmHook.this.customClassName)
                AsmHook.this.hookMethodParameters.add(Type.getType(Object.class));
            else
                AsmHook.this.hookMethodParameters.add(TypeHelper.getType(AsmHook.this.targetClassName));
            AsmHook.this.transmittableVariableIds.add(0);
            return this;
        }

        public Builder addReturnValueToHookMethodParameters() {
            if (!AsmHook.this.hasHookMethod()) {
                throw new IllegalStateException("Hook method is not specified, so can not append " +
                        "parameter to its parameters list.");
            }
            if (AsmHook.this.targetMethodReturnType == Type.VOID_TYPE) {
                throw new IllegalStateException("Target method's return type is void, it does not make sense to " +
                        "transmit its return value to hook method.");
            }
            AsmHook.this.hookMethodParameters.add(AsmHook.this.targetMethodReturnType);
            AsmHook.this.transmittableVariableIds.add(-1);
            AsmHook.this.hasReturnValueParameter = true;
            return this;
        }

        public Builder setReturnCondition(ReturnCondition condition) {
            if (condition.requiresCondition && AsmHook.this.hookMethodName == null) {
                throw new IllegalArgumentException("Hook method is not specified, so can not use return " +
                        "condition that depends on hook method.");
            }

            AsmHook.this.returnCondition = condition;
            AsmHook.this.hookMethodReturnType = condition == NEVER || condition == ALWAYS ? VOID_TYPE : condition == ON_TRUE ? BOOLEAN_TYPE : getType(Object.class);
            return this;
        }

        public Builder setReturnValue(ReturnValue value) {
            if (AsmHook.this.returnCondition == ReturnCondition.NEVER) {
                throw new IllegalStateException("Current return condition is ReturnCondition.NEVER, so it does not " +
                        "make sense to specify the return value.");
            }
            Type returnType = AsmHook.this.targetMethodReturnType;
            if (value != ReturnValue.VOID && returnType == VOID_TYPE) {
                throw new IllegalArgumentException("Target method return value is void, so it does not make sense to " +
                        "return anything else.");
            }
            if (value == ReturnValue.VOID && returnType != VOID_TYPE) {
                throw new IllegalArgumentException("Target method return value is not void, so it is impossible " +
                        "to return VOID.");
            }
            if (value == ReturnValue.PRIMITIVE_CONSTANT && returnType != null && !isPrimitive(returnType)) {
                throw new IllegalArgumentException("Target method return value is not a primitive, so it is " +
                        "impossible to return PRIVITIVE_CONSTANT.");
            }
            if (value == ReturnValue.NULL && returnType != null && isPrimitive(returnType)) {
                throw new IllegalArgumentException("Target method return value is a primitive, so it is impossible " +
                        "to return NULL.");
            }
            if (value == ReturnValue.HOOK_RETURN_VALUE && !hasHookMethod()) {
                throw new IllegalArgumentException("Hook method is not specified, so can not use return " +
                        "value that depends on hook method.");
            }

            AsmHook.this.returnValue = value;
            if (value == ReturnValue.HOOK_RETURN_VALUE) {
                AsmHook.this.hookMethodReturnType = AsmHook.this.targetMethodReturnType;
            }
            return this;
        }

        public Type getHookMethodReturnType() {
            return hookMethodReturnType;
        }

        public void setHookMethodReturnType(Type type) {
            AsmHook.this.hookMethodReturnType = type;
        }

        public boolean isPrimitive(Type type) {
            return type.getSort() > 0 && type.getSort() < 9;
        }

        public Builder setPrimitiveConstant(Object constant) {
            if (AsmHook.this.returnValue != ReturnValue.PRIMITIVE_CONSTANT) {
                throw new IllegalStateException("Return value is not PRIMITIVE_CONSTANT, so it does not make sence" +
                        "to specify that constant.");
            }
            Type returnType = AsmHook.this.targetMethodReturnType;
            if (returnType == BOOLEAN_TYPE && !(constant instanceof Boolean) ||
                    returnType == CHAR_TYPE && !(constant instanceof Character) ||
                    returnType == BYTE_TYPE && !(constant instanceof Byte) ||
                    returnType == SHORT_TYPE && !(constant instanceof Short) ||
                    returnType == INT_TYPE && !(constant instanceof Integer) ||
                    returnType == LONG_TYPE && !(constant instanceof Long) ||
                    returnType == FLOAT_TYPE && !(constant instanceof Float) ||
                    returnType == DOUBLE_TYPE && !(constant instanceof Double)) {
                throw new IllegalArgumentException("Given object class does not match target method return type.");
            }

            AsmHook.this.primitiveConstant = constant;
            return this;
        }

        public Builder setReturnMethod(String methodName) {
            if (AsmHook.this.returnValue != ReturnValue.ANOTHER_METHOD_RETURN_VALUE) {
                throw new IllegalStateException("Return value is not ANOTHER_METHOD_RETURN_VALUE, " +
                        "so it does not make sence to specify that method.");
            }

            AsmHook.this.returnMethodName = methodName;
            return this;
        }

        public Builder setInjectorFactory(HookInjectorFactory factory) {
            AsmHook.this.injectorFactory = factory;
            return this;
        }

        public Builder setInjectorSettings(String[] settings) {
            AsmHook.this.injectorSettings = settings;
            return this;
        }

        public Builder setPriority(HookPriority priority) {
            AsmHook.this.priority = priority;
            return this;
        }

        public Builder setCreateMethod(boolean createMethod) {
            AsmHook.this.createMethod = createMethod;
            return this;
        }

        public Builder setMandatory(boolean isMandatory) {
            AsmHook.this.isMandatory = isMandatory;
            return this;
        }

        public String getMethodDesc(Type returnType, List<Type> paramTypes) {
            Type[] paramTypesArray = paramTypes.toArray(new Type[0]);
            if (returnType == null) {
                String voidDesc = Type.getMethodDescriptor(Type.VOID_TYPE, paramTypesArray);
                return voidDesc.substring(0, voidDesc.length() - 1);
            } else {
                return Type.getMethodDescriptor(returnType, paramTypesArray);
            }
        }

        public AsmHook build() {
            AsmHook hook = AsmHook.this;

            if (hook.createMethod && hook.targetMethodReturnType == null) {
                hook.targetMethodReturnType = hook.hookMethodReturnType;
            }
            hook.targetMethodDescription = getMethodDesc(hook.targetMethodReturnType, hook.targetMethodParameters);

            if (hook.hasHookMethod()) {
                hook.hookMethodDescription = Type.getMethodDescriptor(hook.hookMethodReturnType,
                        hook.hookMethodParameters.toArray(new Type[0]));
            }
            if (hook.returnValue == ReturnValue.ANOTHER_METHOD_RETURN_VALUE) {
                hook.returnMethodDescription = getMethodDesc(hook.targetMethodReturnType, hook.hookMethodParameters);
            }

            try {
                hook = (AsmHook) AsmHook.this.clone();
            } catch (CloneNotSupportedException impossible) {
            }

            if (hook.targetClassName == null) {
                throw new IllegalStateException("Target class name is not specified. " +
                        "Call setTargetClassName() before build().");
            }

            if (hook.targetMethodName == null) {
                throw new IllegalStateException("Target method name is not specified. " +
                        "Call setTargetMethodName() before build().");
            }

            if (hook.returnValue == ReturnValue.PRIMITIVE_CONSTANT && hook.primitiveConstant == null) {
                throw new IllegalStateException("Return value is PRIMITIVE_CONSTANT, but the constant is not " +
                        "specified. Call setReturnValue() before build().");
            }

            if (hook.returnValue == ReturnValue.ANOTHER_METHOD_RETURN_VALUE && hook.returnMethodName == null) {
                throw new IllegalStateException("Return value is ANOTHER_METHOD_RETURN_VALUE, but the method is not " +
                        "specified. Call setReturnMethod() before build().");
            }

            if (hook.injectorFactory != HookInjectorFactory.exit && hook.hasReturnValueParameter) {
                throw new IllegalStateException("Can not pass return value to hook method " +
                        "because hook location is not return insn.");
            }

            return hook;
        }
    }
}

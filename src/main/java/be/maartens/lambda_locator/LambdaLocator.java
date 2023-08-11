package com.guardsquare.lambda_locator;

import proguard.backport.LambdaExpression;
import proguard.backport.LambdaExpressionCollector;
import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.AllInstructionVisitor;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.ClassReferenceInitializer;
import proguard.classfile.visitor.MemberVisitor;

import java.util.*;

public class LambdaLocator {
    private final Map<Clazz, Map<Method, Set<Lambda>>> classLambdas = new HashMap<>();
    private final List<Lambda> staticLambdas = new ArrayList<>();
    private final Map<Integer, Lambda> staticLambdaMap = new HashMap<>();
    private final Set<Clazz> lambdaClasses = new HashSet<>();
    private final Set<Clazz> synthLambdaClasses = new HashSet<>();
    public LambdaLocator(ClassPool classPool, String classNameFilter) {
        //TODO:new ClassReferenceInitializer(programClassPool, libraryClassPool)
        //ClassInitializer

        /*classPool.classesAccept(
                new ClassReferenceInitializer(classPool, new ClassPool())
        );*/

        classPool.classesAccept(classNameFilter, clazz -> {
            // Find classes that inherit from kotlin.jvm.internal.Lambda
            clazz.superClassConstantAccept(new ConstantVisitor() {
                @Override
                public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
                    clazz.constantPoolEntryAccept(classConstant.u2nameIndex, new ConstantVisitor() {
                        @Override
                        public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
                            if (utf8Constant.getString().equals("kotlin/jvm/internal/Lambda")) {
                                System.out.println("Class " + clazz.getName() + " is a kotlin lambda class!");
                                lambdaClasses.add(clazz);
                            } else {
                                System.out.println("Class " + clazz.getName() + " is not a kotlin lambda class!");
                            }
                        }
                    });
                }
            });

            clazz.interfaceConstantsAccept(new ConstantVisitor() {
                @Override
                public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
                    clazz.constantPoolEntryAccept(classConstant.u2nameIndex, new ConstantVisitor() {
                        @Override
                        public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
                            classPool.classAccept(utf8Constant.getString(), referencedClazz -> {
                                referencedClazz.interfaceConstantsAccept(new ConstantVisitor() {
                                    @Override
                                    public void visitClassConstant(Clazz referencedClazz, ClassConstant classConstant) {
                                        referencedClazz.constantPoolEntryAccept(classConstant.u2nameIndex, new ConstantVisitor() {
                                            @Override
                                            public void visitUtf8Constant(Clazz referencedClazz, Utf8Constant utf8Constant) {
                                                if (utf8Constant.getString().equals("kotlin/Function")) {
                                                    //synthLambdaClasses.add(clazz);

                                                    // Check if the class is synthetic, we do this because regular lambda classes
                                                    // are also sometimes instantiated because they use a singleton system for
                                                    // example.
                                                    clazz.attributesAccept(new AttributeVisitor() {
                                                        @Override
                                                        public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

                                                        @Override
                                                        public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute) {
                                                            System.out.println(sourceFileAttribute);
                                                            clazz.constantPoolEntryAccept(sourceFileAttribute.u2sourceFileIndex, new ConstantVisitor() {
                                                                @Override
                                                                public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
                                                                    if (utf8Constant.getString().equals("D8$$SyntheticClass")) {
                                                                        System.out.println(utf8Constant);
                                                                        synthLambdaClasses.add(clazz);
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                });
                            });
                        }
                    });
                    //TODO: Figure out how to use classConstant.referencedClass instead of following the name
                    // and then getting it from the classPool, this can be done using the ClassReferenceInitializer,
                    // this works sometimes, it works on the example apk but if you try using a jar file it does not
                    // work anymore.
                    /*classConstant.referencedClass.interfaceConstantsAccept(new ConstantVisitor() {
                        @Override
                        public void visitClassConstant(Clazz referencedClazz, ClassConstant classConstant) {
                            referencedClazz.constantPoolEntryAccept(classConstant.u2nameIndex, new ConstantVisitor() {
                                @Override
                                public void visitUtf8Constant(Clazz referencedClazz, Utf8Constant utf8Constant) {
                                    if (utf8Constant.getString().equals("kotlin/Function")) {
                                        //synthLambdaClasses.add(clazz);

                                        // Check if the class is synthetic, we do this because regular lambda classes
                                        // are also sometimes instantiated because they use a singleton system for
                                        // example.
                                        clazz.attributesAccept(new AttributeVisitor() {
                                            @Override
                                            public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

                                            @Override
                                            public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute) {
                                                System.out.println(sourceFileAttribute);
                                                clazz.constantPoolEntryAccept(sourceFileAttribute.u2sourceFileIndex, new ConstantVisitor() {
                                                    @Override
                                                    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
                                                        if (utf8Constant.getString().equals("D8$$SyntheticClass")) {
                                                            System.out.println(utf8Constant);
                                                            synthLambdaClasses.add(clazz);
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });*/
                }
            });
        });

        // Find bootstrap method attributes and constants
        classPool.classesAccept(classNameFilter, clazz -> {

            List<BootstrapMethodsAttribute> bootstrapMethodsAttributes = new ArrayList<>();

            clazz.attributesAccept(new AttributeVisitor() {
                @Override
                public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

                @Override
                public void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute) {
                   bootstrapMethodsAttributes.add(bootstrapMethodsAttribute);
                }

                @Override
                public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute) {}
            });

            HashMap<Integer, LambdaExpression> h = new HashMap<>();
            LambdaExpressionCollector lec = new LambdaExpressionCollector(h);
            lec.visitProgramClass((ProgramClass) clazz);
            Set<Integer> bootstrapIndices = new HashSet<>(h.keySet());

            clazz.methodsAccept(new MemberVisitor() {
                @Override
                public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {
                    programMethod.accept(programClass, new AllAttributeVisitor(new AllInstructionVisitor(new InstructionVisitor() {
                        @Override
                        public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}

                        @Override
                        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction) {
                            if (constantInstruction.opcode == Instruction.OP_INVOKEDYNAMIC) {
                                clazz.constantPoolEntryAccept(constantInstruction.constantIndex, new ConstantVisitor() {
                                    @Override
                                    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant) {
                                        int bootStrapMethodIndex = invokeDynamicConstant.u2bootstrapMethodAttributeIndex;
                                        if (bootstrapIndices.contains(bootStrapMethodIndex)) {
                                            System.out.println("Is a lambda invocation!");
                                            clazz.constantPoolEntryAccept(invokeDynamicConstant.u2nameAndTypeIndex, new ConstantVisitor() {
                                                @Override
                                                public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant) {
                                                    clazz.constantPoolEntryAccept(nameAndTypeConstant.u2descriptorIndex, new ConstantVisitor() {
                                                        @Override
                                                        public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
                                                            System.out.println("Function signature: " + utf8Constant.getString());

                                                            classLambdas.putIfAbsent(clazz, new HashMap<>());
                                                            classLambdas.get(clazz).putIfAbsent(method, new HashSet<>());
                                                            classLambdas.get(clazz).get(method).add(new Lambda(clazz, method, codeAttribute, offset, constantInstruction));
                                                        }
                                                    });
                                                }
                                            });

                                        } else {
                                            System.out.println("Is not lambda invocation!");
                                        }
                                    }
                                });
                            }

                            if (constantInstruction.opcode == Instruction.OP_GETSTATIC) {
                                clazz.constantPoolEntryAccept(constantInstruction.constantIndex, new ConstantVisitor() {
                                    @Override
                                    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant) {
                                        clazz.constantPoolEntryAccept(fieldrefConstant.u2classIndex, new ConstantVisitor() {
                                            @Override
                                            public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
                                                clazz.constantPoolEntryAccept(classConstant.u2nameIndex, new ConstantVisitor() {
                                                    @Override
                                                    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
                                                        classPool.classAccept(utf8Constant.getString(), referencedClazz -> {
                                                            if (lambdaClasses.contains(referencedClazz)) {
                                                                System.out.println("Found a lambda invocation " + constantInstruction);

                                                                classLambdas.putIfAbsent(clazz, new HashMap<>());
                                                                classLambdas.get(clazz).putIfAbsent(method, new HashSet<>());
                                                                classLambdas.get(clazz).get(method).add(new Lambda(clazz, method, codeAttribute, offset, constantInstruction));

                                                                Lambda lambda = new Lambda(clazz, method, codeAttribute, offset, constantInstruction);
                                                                staticLambdas.add(lambda);
                                                                staticLambdaMap.put(lambda.constantInstruction.constantIndex, lambda);
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }

                            // Check for init of synthetic lambda classes
                            if (constantInstruction.opcode == Instruction.OP_INVOKESPECIAL) {
                                clazz.constantPoolEntryAccept(constantInstruction.constantIndex, new ConstantVisitor() {
                                    @Override
                                    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant) {
                                        clazz.constantPoolEntryAccept(anyMethodrefConstant.u2classIndex, new ConstantVisitor() {
                                            @Override
                                            public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
                                                clazz.constantPoolEntryAccept(classConstant.u2nameIndex, new ConstantVisitor() {
                                                    @Override
                                                    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
                                                        classPool.classAccept(utf8Constant.getString(), referencedClazz -> {
                                                            if (synthLambdaClasses.contains(referencedClazz)) {
                                                                System.out.println("Found a lambda invocation " + constantInstruction);

                                                                classLambdas.putIfAbsent(clazz, new HashMap<>());
                                                                classLambdas.get(clazz).putIfAbsent(method, new HashSet<>());
                                                                classLambdas.get(clazz).get(method).add(new Lambda(clazz, method, codeAttribute, offset, constantInstruction));
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    })));
                }
            });
        });
    }

    public Map<Clazz, Map<Method, Set<Lambda>>> getLambdasByClass() {
        return classLambdas;
    }

    public List<Lambda> getStaticLambdas() {
        return staticLambdas;
    }

    public Map<Integer, Lambda> getStaticLambdaMap() {
        return staticLambdaMap;
    }

    public record Lambda(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction) {
        @Override
        public String toString() {
            return constantInstruction.toString();
        }
    }
}

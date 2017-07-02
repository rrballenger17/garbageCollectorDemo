package edu.harvard.cscie98.simplejava.impl.interpreter.bytecodes;

import java.util.List;

import edu.harvard.cscie98.simplejava.config.Log;
import edu.harvard.cscie98.simplejava.impl.interpreter.InterpreterUtils;
import edu.harvard.cscie98.simplejava.vm.classloader.TypeName;
import edu.harvard.cscie98.simplejava.vm.classloader.VmClass;
import edu.harvard.cscie98.simplejava.vm.classloader.VmClassLoader;
import edu.harvard.cscie98.simplejava.vm.classloader.VmConstantPool;
import edu.harvard.cscie98.simplejava.vm.classloader.VmMethod;
import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapPointer;
import edu.harvard.cscie98.simplejava.vm.objectmodel.ObjectBuilder;
import edu.harvard.cscie98.simplejava.vm.threads.JvmStack;
import edu.harvard.cscie98.simplejava.vm.threads.StackFrame;

public class InvokespecialInst {

  public static void interpret(final int pc, final byte[] code, final JvmStack stack,
      final StackFrame frame, final VmClassLoader classLoader,
      final ObjectBuilder objectBuilder) {
    final int methodIdx = InterpreterUtils.getUnsignedDoubleByte(code, pc + 1);
    final VmConstantPool constantPool = frame.getConstantPool();
    final TypeName className = InterpreterUtils.getMethodClass(constantPool, methodIdx);
    final String methodName = InterpreterUtils.getMethodName(constantPool, methodIdx);
    final String methodSig = InterpreterUtils.getMethodSignature(constantPool, methodIdx);
    final VmClass cls = classLoader.loadClass(className);
    final VmMethod mthd = cls.getMethod(methodName, methodSig);

    final List<TypeName> params = mthd.getParamters();
    final StackFrame newFrame = stack.push(mthd);
    for (int i = params.size(); i > 0; i--) {
      newFrame.setLocalVariable(i, frame.pop());
    }
    final HeapPointer objref = (HeapPointer) frame.pop();
    newFrame.setLocalVariable(0, objref);
    frame.setProgramCounter(pc + 3);
    Log.interpreter("Invoking " + mthd.getDefiningClass() + "." + mthd);
  }
}

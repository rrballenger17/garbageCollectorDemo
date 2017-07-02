package edu.harvard.cscie98.simplejava.impl.interpreter.bytecodes;

import java.util.List;

import edu.harvard.cscie98.simplejava.config.Log;
import edu.harvard.cscie98.simplejava.impl.interpreter.InterpreterUtils;
import edu.harvard.cscie98.simplejava.vm.classloader.TypeName;
import edu.harvard.cscie98.simplejava.vm.classloader.VmClassLoader;
import edu.harvard.cscie98.simplejava.vm.classloader.VmMethod;
import edu.harvard.cscie98.simplejava.vm.objectmodel.ObjectBuilder;
import edu.harvard.cscie98.simplejava.vm.threads.JvmStack;
import edu.harvard.cscie98.simplejava.vm.threads.StackFrame;

public class InvokestaticInst {

  public static void interpret(final int pc, final byte[] code, final JvmStack stack,
      final StackFrame frame, final VmClassLoader classLoader, final ObjectBuilder objectBuilder) {
    final VmMethod mthd = InterpreterUtils.getStaticMethodRef(pc, code, frame, classLoader);
    final List<TypeName> params = mthd.getParamters();
    InterpreterUtils.createStackFrame(stack, frame, mthd, params.size());
    frame.setProgramCounter(pc + 3);
    Log.interpreter("Invoking " + mthd.getDefiningClass() + "." + mthd);
  }
}

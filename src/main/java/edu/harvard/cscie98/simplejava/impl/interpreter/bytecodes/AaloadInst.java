package edu.harvard.cscie98.simplejava.impl.interpreter.bytecodes;

import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapPointer;
import edu.harvard.cscie98.simplejava.vm.objectmodel.ObjectBuilder;
import edu.harvard.cscie98.simplejava.vm.threads.JvmStack;
import edu.harvard.cscie98.simplejava.vm.threads.StackFrame;

public class AaloadInst {

  public static void interpret(final int pc, final byte[] code, final JvmStack stack,
      final StackFrame frame, final ObjectBuilder objectBuilder) {
    final int index = (int) frame.pop();
    final HeapPointer arrayRef = (HeapPointer) frame.pop();
    final Object value = arrayRef.dereference().getValueAtOffset(index);
    frame.push(value);
    frame.setProgramCounter(pc + 1);
  }
}

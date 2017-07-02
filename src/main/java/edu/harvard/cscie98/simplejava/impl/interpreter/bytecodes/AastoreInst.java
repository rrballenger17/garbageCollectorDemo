package edu.harvard.cscie98.simplejava.impl.interpreter.bytecodes;

import edu.harvard.cscie98.simplejava.vm.memory.WriteBarrier;
import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapObject;
import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapPointer;
import edu.harvard.cscie98.simplejava.vm.objectmodel.ObjectBuilder;
import edu.harvard.cscie98.simplejava.vm.threads.JvmStack;
import edu.harvard.cscie98.simplejava.vm.threads.StackFrame;

public class AastoreInst {

  public static void interpret(final int pc, final byte[] code, final JvmStack stack,
      final StackFrame frame, final ObjectBuilder objectBuilder, final WriteBarrier barrier) {
    final HeapPointer val = (HeapPointer) frame.pop();
    final int index = (int) frame.pop();
    final HeapPointer arrayRef = (HeapPointer) frame.pop();
    final HeapObject obj = arrayRef.dereference();
    obj.setValueAtOffset(index, val);
    barrier.onPointerWrite(obj, val);
    frame.setProgramCounter(pc + 1);
  }
}

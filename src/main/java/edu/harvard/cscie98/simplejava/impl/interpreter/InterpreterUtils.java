package edu.harvard.cscie98.simplejava.impl.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;

import edu.harvard.cscie98.simplejava.impl.classloader.PrimordialMainMethod;
import edu.harvard.cscie98.simplejava.vm.classloader.TypeFactory;
import edu.harvard.cscie98.simplejava.vm.classloader.TypeName;
import edu.harvard.cscie98.simplejava.vm.classloader.VmClass;
import edu.harvard.cscie98.simplejava.vm.classloader.VmClassLoader;
import edu.harvard.cscie98.simplejava.vm.classloader.VmConstantPool;
import edu.harvard.cscie98.simplejava.vm.classloader.VmField;
import edu.harvard.cscie98.simplejava.vm.classloader.VmMethod;
import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapObject;
import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapPointer;
import edu.harvard.cscie98.simplejava.vm.objectmodel.ObjectBuilder;
import edu.harvard.cscie98.simplejava.vm.threads.JvmStack;
import edu.harvard.cscie98.simplejava.vm.threads.StackFrame;

public class InterpreterUtils {

  public static final int getByteImmediate(final byte[] code, final int idx) {
    return code[idx];
  }

  public static final int getByteUnsigned(final byte[] code, final int idx) {
    final byte b = code[idx];
    final int i = b & 0xFF;
    return i;
  }

  public static final int getSignedDoubleByte(final byte[] code, final int idx) {
    final byte one = code[idx];
    final byte two = code[idx + 1];
    int val = one;
    val <<= 8;
    val |= (two & 0xFF);
    return val;
  }

  public static int getUnsignedDoubleByte(final byte[] code, final int idx) {
    final int one = getByteUnsigned(code, idx);
    final int two = getByteUnsigned(code, idx + 1);
    int val = one << 8;
    val |= two;
    return val;
  }

  private static ConstantNameAndType getMethodNameType(final VmConstantPool constantPool,
      final int cpIndex) {
    final ConstantMethodref methodEntry = constantPool.getMethodEntry(cpIndex);
    return constantPool.getNameAndTypeEntry(methodEntry.getNameAndTypeIndex());
  }

  public static String getMethodName(final VmConstantPool constantPool, final int cpIndex) {
    final ConstantNameAndType methodNt = getMethodNameType(constantPool, cpIndex);
    return constantPool.getUtf8Entry(methodNt.getNameIndex());
  }

  public static String getMethodSignature(final VmConstantPool constantPool, final int cpIndex) {
    final ConstantNameAndType methodNt = getMethodNameType(constantPool, cpIndex);
    return constantPool.getUtf8Entry(methodNt.getSignatureIndex());
  }

  public static TypeName getMethodClass(final VmConstantPool constantPool, final int cpIndex) {
    final ConstantMethodref methodEntry = constantPool.getMethodEntry(cpIndex);
    return constantPool.getClassEntry(methodEntry.getClassIndex());
  }

  private static ConstantNameAndType getFieldNameType(final VmConstantPool constantPool,
      final int cpIndex) {
    final ConstantFieldref fieldEntry = constantPool.getFieldEntry(cpIndex);
    return constantPool.getNameAndTypeEntry(fieldEntry.getNameAndTypeIndex());
  }

  public static String getFieldName(final VmConstantPool constantPool, final int cpIndex) {
    final ConstantNameAndType fieldNt = getFieldNameType(constantPool, cpIndex);
    return constantPool.getUtf8Entry(fieldNt.getNameIndex());
  }

  public static TypeName getFieldSignature(final VmConstantPool constantPool, final int cpIndex) {
    final ConstantNameAndType fieldNt = getFieldNameType(constantPool, cpIndex);
    return TypeFactory.fromDescriptor(constantPool.getUtf8Entry(fieldNt.getSignatureIndex()));
  }

  public static TypeName getFieldClass(final VmConstantPool constantPool, final int cpIndex) {
    final ConstantFieldref fieldEntry = constantPool.getFieldEntry(cpIndex);
    return constantPool.getClassEntry(fieldEntry.getClassIndex());
  }

  public static Object getOrSetStatic(final int pc, final byte[] code, final StackFrame frame,
      final VmClassLoader classLoader, final Object val) {
    final VmConstantPool constantPool = frame.getConstantPool();
    final int fieldIdx = InterpreterUtils.getUnsignedDoubleByte(code, pc + 1);
    final TypeName className = InterpreterUtils.getFieldClass(constantPool, fieldIdx);
    final String fieldName = InterpreterUtils.getFieldName(constantPool, fieldIdx);
    final TypeName fieldSignature = getFieldSignature(constantPool, fieldIdx);
    final VmClass cls = classLoader.loadClass(className);
    if (val != null) {
      cls.getObjectTypeDescriptor().setStaticField(fieldName, fieldSignature, val);
      return null;
    } else {
      return cls.getObjectTypeDescriptor().getStaticField(fieldName, fieldSignature);
    }
  }

  public static void initializeStackTrace(final HeapPointer throwable,
      final TypeName exceptionType, final ObjectBuilder builder) {
    final HeapObject internedTypeName = builder.internString(exceptionType.getBinaryName());
    final HeapObject t = throwable.dereference();
    t.setValueAtOffset(0, internedTypeName.getAddress());
  }

  public static void addLineToStackTrace(final HeapPointer throwable, final VmMethod mthd,
      final String file, final int lineNumber, final ObjectBuilder builder) {
    if (mthd instanceof PrimordialMainMethod) {
      return;
    }
    final HeapObject t = throwable.dereference();
    final HeapPointer tracePtr = (HeapPointer) t.getValueAtOffset(1);
    final HeapObject trace = tracePtr.dereference();
    for (int i = 0; i < 1024; i++) {
      if (trace.getValueAtOffset(i) == HeapPointer.NULL) {
        final String line = "\tat " + mthd.getDefiningClass().getName() + "." + mthd.getName()
        + "(" + file + ":" + lineNumber + ")";
        final HeapObject interned = builder.internString(line);
        trace.setValueAtOffset(i, interned.getAddress());
        return;
      }
    }
    final String line = "...";
    final HeapObject interned = builder.internString(line);
    trace.setValueAtOffset(1023, interned.getAddress());
  }

  public static List<TypeName> getParamsFromDescriptor(final String desc) {
    int idx = 1;
    final List<TypeName> params = new ArrayList<TypeName>();
    while (desc.charAt(idx) != ')') {
      int dim = 0;
      while (desc.charAt(idx) == '[') {
        dim++;
        idx++;
      }
      if (desc.charAt(idx) == 'L') {
        final int typeStart = idx + 1;
        int typeEnd = typeStart;
        while (desc.charAt(idx++) != ';') {
          typeEnd++;
        }
        final String className = desc.substring(typeStart, typeEnd - 1);
        params.add(TypeFactory.fromClassFileName(className, dim > 0));
      } else {
        String descriptor = "";
        if (dim > 0) {
          descriptor += "[";
        }
        descriptor += desc.charAt(idx++);
        params.add(TypeFactory.fromDescriptor(descriptor));
      }
    }
    return params;
  }

  public static VmMethod getStaticMethodRef(final int pc, final byte[] code,
      final StackFrame frame, final VmClassLoader classLoader) {
    final int methodIdx = InterpreterUtils.getUnsignedDoubleByte(code, pc + 1);
    final VmConstantPool constantPool = frame.getConstantPool();
    final TypeName className = InterpreterUtils.getMethodClass(constantPool, methodIdx);
    final String methodName = InterpreterUtils.getMethodName(constantPool, methodIdx);
    final String methodSig = InterpreterUtils.getMethodSignature(constantPool, methodIdx);
    final VmClass cls = classLoader.loadClass(className);
    return cls.getMethod(methodName, methodSig);
  }

  public static void createStackFrame(final JvmStack stack, final StackFrame frame,
      final VmMethod mthd, final int paramCount) {
    final StackFrame newFrame = stack.push(mthd);
    for (int i = paramCount - 1; i >= 0; i--) {
      newFrame.setLocalVariable(i, frame.pop());
    }
  }

  public static final int getFieldIndex(final int pc, final byte[] code, final StackFrame frame,
      final VmClassLoader classLoader) {
    final VmConstantPool constantPool = frame.getConstantPool();
    final int fieldIdx = InterpreterUtils.getUnsignedDoubleByte(code, pc + 1);
    final TypeName className = InterpreterUtils.getFieldClass(constantPool, fieldIdx);
    VmClass cls = classLoader.loadClass(className);
    final String fieldName = InterpreterUtils.getFieldName(constantPool, fieldIdx);
    final TypeName fieldSignature = InterpreterUtils.getFieldSignature(constantPool, fieldIdx);
    VmField fld = cls.getField(fieldName, fieldSignature);
    while (fld == null) {
      cls = cls.getSuperClass();
      if (cls == null) {
        throw new NoSuchFieldError();
      }
      fld = cls.getField(fieldName, fieldSignature);
    }
    return cls.getObjectTypeDescriptor().getFieldOffset(cls, fld);
  }

}

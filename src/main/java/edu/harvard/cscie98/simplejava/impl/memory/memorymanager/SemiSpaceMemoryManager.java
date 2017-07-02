package edu.harvard.cscie98.simplejava.impl.memory.memorymanager;
import edu.harvard.cscie98.simplejava.config.HeapParameters;
import edu.harvard.cscie98.simplejava.vm.classloader.VmClassLoader;
import edu.harvard.cscie98.simplejava.vm.memory.MemoryManager;
import edu.harvard.cscie98.simplejava.vm.memory.WriteBarrier;
import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapPointer;
import edu.harvard.cscie98.simplejava.vm.objectmodel.ObjectBuilder;
import edu.harvard.cscie98.simplejava.vm.threads.JvmThread;
import edu.harvard.cscie98.simplejava.vm.memory.*;
import edu.harvard.cscie98.simplejava.vm.objectmodel.*;
import edu.harvard.cscie98.simplejava.vm.classloader.*;
import java.util.*;
public class SemiSpaceMemoryManager implements MemoryManager {

	private JvmThread myThread = null;
	private ObjectBuilder myObjectBuilder = null;
	private VmClassLoader myClassLoader = null;

	private Heap heap = null;
	private HeapParameters heapParameters;
	private final Region regionOne;
	private final Region regionTwo;
	private boolean regionOneBool = true;

	private List<HeapPointer> objectsToScan = new ArrayList<HeapPointer>();

	private boolean inCollection = false;

	public SemiSpaceMemoryManager(final HeapParameters heapParams) {
		//throw new RuntimeException("TODO: Your implementation for Assignment 2 goes here");
		long heapSize = heapParams.getExtent();
		regionOne = heapParams.getHeap().getBumpPointerRegion(heapParams.getBaseAddress(), heapSize/2);
		regionTwo = heapParams.getHeap().getBumpPointerRegion(heapParams.getBaseAddress().add(heapSize/2), heapSize/2);
		heapParameters = heapParams;
		heap = heapParams.getHeap();

	}

	@Override
	public void setThread(final JvmThread thread) {
		myThread = thread;
	}
	@Override
	public void setObjectBuilder(final ObjectBuilder objectBuilder) {
		myObjectBuilder = objectBuilder;
	}
	@Override
	public void setClassLoader(final VmClassLoader classLoader) {
		myClassLoader = classLoader;
	}
	@Override
	public void setBarrier(final WriteBarrier barrier) {
		// Note: There is no write barrier required for the semi-space memory manager
	}

	private HeapPointer allocateBytes(final long bytes){

		if(regionOneBool) {
			return regionOne.allocate(bytes);
	  	} 
	  
	  	return regionTwo.allocate(bytes);
	}


	@Override
	public HeapPointer allocate(final long bytes) {

		HeapPointer allocated = allocateBytes(bytes);

		if(allocated == HeapPointer.NULL) {
			  garbageCollect();
			  allocated = allocateBytes(bytes);
		}

		return allocated;
	}


	private void scanAndUpdate(HeapPointer hp) {

		HeapObject ho = hp.dereference();
		ObjectHeader oh = ho.getHeader();
		TypeDescriptor td = (TypeDescriptor)oh.getWord(ObjectHeader.CLASS_DESCRIPTOR_WORD);

		if(td.isObject()) {

			ObjectTypeDescriptor otd = (ObjectTypeDescriptor) td;
			List<VmField> list = new ArrayList<VmField>();
			list.addAll(otd.getFields());

			ObjectTypeDescriptor superOtd = otd.getSuperClassDescriptor();
			while(superOtd != null){
				List<VmField> newList = superOtd.getFields();
				list.addAll(newList);

				superOtd = superOtd.getSuperClassDescriptor();
			}

			for (VmField l: list) {
				int offset = otd.getFieldOffset(l.getDefiningClass(), l);

				if(l.isReference()) {
					HeapPointer internalPointer = (HeapPointer)ho.getValueAtOffset(offset);
					ho.setValueAtOffset(offset, processHeapPointer(internalPointer) );
				}
			}
		} else if(td.isArray()) {
			
			int arraySize = (int)oh.getWord(ObjectHeader.ARRAY_LENGTH_WORD);
			ArrayTypeDescriptor atd = (ArrayTypeDescriptor) td;
			
			if(atd.getElementDescriptor().isObject()) {
				for (int i=0; i<arraySize; i++) {
					HeapPointer newHP = (HeapPointer)ho.getValueAtOffset(i);
					ho.setValueAtOffset(i, processHeapPointer(newHP));
				}
			} else if(atd.getElementDescriptor().isArray()) {
				for (int i=0; i<arraySize; i++) {
					//System.out.println("------Error: array of arrays aren't supported-------\n");
					//System.exit(1);
					return;
				}
			} else {
				if(!atd.getElementDescriptor().isPrimitive()) {
					//System.out.println("-----Error: array content has no type --------\n");
					//System.exit(1);
					return;
				}
			}
		} else {
			if(!td.isPrimitive()) {
				//System.out.println("----Error: heap object has no type-----");
				//System.exit(1);
				return;
			}
		}
	}

	private HeapPointer copyObject(HeapPointer hp) {
		HeapObject ho = hp.dereference();
		HeapPointer newHP = allocate(ho.getSize());
		heap.memcpy(hp, newHP);
		return newHP;
	}

	private HeapPointer processHeapPointer(HeapPointer hp) {
		if(hp == HeapPointer.NULL) {
			return HeapPointer.NULL;
		}

		// forwarding pointer exists 
		HeapObject ho = hp.dereference();
		ObjectHeader oh = ho.getHeader();
		if(oh.getWord(ObjectHeader.CLASS_DESCRIPTOR_WORD) instanceof HeapPointer) {
			// update reference location and be done 
			return (HeapPointer) oh.getWord(ObjectHeader.CLASS_DESCRIPTOR_WORD);
		}

		// copy
		HeapPointer copy = copyObject(hp);

		// add forwarding pointer 
		oh.setWord(ObjectHeader.CLASS_DESCRIPTOR_WORD, copy);
		
		objectsToScan.add(copy);

		return copy;
	}


	@Override
	public void garbageCollect() {

		inCollection = true;

		//System.out.println("garbage");

		regionOneBool = !regionOneBool;
		if(regionOneBool) {
			regionOne.reset();
		} else {
			regionTwo.reset();
		}

		List<ReferenceLocation> list = myThread.getStack().getStackAndLocalReferenceLocations();
		for (ReferenceLocation rl: list) {
			rl.setValue(processHeapPointer(rl.getValue()));
		}
		//System.out.println("JVMThread (variables): " + list.size());



		List<ReferenceLocation> listTwo = myObjectBuilder.getInternTableReferences();
		for (ReferenceLocation rl: listTwo) {
			rl.setValue(processHeapPointer(rl.getValue()));
		}
		//System.out.println("ObjectBuilder (strings) : " + listTwo.size());



		List<ReferenceLocation> listThree = myClassLoader.getStaticReferenceLocations();
		for (ReferenceLocation rl: listThree) {
			rl.setValue(processHeapPointer(rl.getValue()));
		}
		//System.out.println("ClassLoader (statics): " + listThree.size());


		while(objectsToScan.size() > 0){
			
			HeapPointer hp = objectsToScan.get(0);

			objectsToScan.remove(0);

			scanAndUpdate(hp);

		}

		inCollection = false;

	}
}














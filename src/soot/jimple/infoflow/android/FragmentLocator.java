package soot.jimple.infoflow.android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.Transform;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointConstants;
import soot.util.Chain;

/**
 * Locate the Fragments used the APK 
 * 
 * @author Wil Koch
 *
 */
public class FragmentLocator {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private List<String> fragmentClasses;

	private Set<String> entrypoints = null;

	public FragmentLocator(){
		this.fragmentClasses = new ArrayList<String>();
		fragmentClasses.add(AndroidEntryPointConstants.V4FRAGMENTCLASS);
		fragmentClasses.add(AndroidEntryPointConstants.FRAGMENTCLASS);
	}
	
	public void collectFragments(){
		this.entrypoints = new HashSet<String>();

		Chain<SootClass> classes = Scene.v().getClasses();
		Iterator<SootClass> it = classes.iterator();
		while (it.hasNext()){
			SootClass sootClass = it.next();
			//Ignore android framework
			if (!sootClass.getName().startsWith("android") && hasSuperClass(sootClass, fragmentClasses)){
				logger.debug("Found fragment class {}", sootClass.getName());
				this.entrypoints.add(sootClass.getName());
			}
		}
		
	}
	
	/**
	 * Get a set containing the Fragment class names used in the apk
	 * @return
	 */
	public Set<String> getFragmentClasses(){
		return this.entrypoints;
	}
	
	/**
	 * Go up the hierarchy and see if it has a parent 
	 * @param sootClass
	 * @param superClass
	 * @return
	 */
	private boolean hasSuperClass(SootClass sootClass, List<String> superClasses){
		
		String className = sootClass.getName();
		if (className.equals("java.lang.Object")){
			return false;
		} else if (superClasses.contains(className)){
			return true;
		} else {
			SootClass superClass = sootClass.getSuperclass();
			if (superClass == null){
				return false;
			} else {
				return hasSuperClass(superClass, superClasses);
			}
		}
		//SootClass currentClass = Scene.v().getSootClass(entry.getKey());

		//List<SootClass> extendedClasses = Scene.v().getActiveHierarchy().getSuperclassesOf(currentClass);
		//for(SootClass sc : extendedClasses) {
		
	}
	
}

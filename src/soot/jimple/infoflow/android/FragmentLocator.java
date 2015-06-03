package soot.jimple.infoflow.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.VirtualInvokeExpr;
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

	//Key is the package name containing a histogram of the number of characters for each class
	SortedMap<String,TreeMap<Integer, Integer>> classNameHistogram = new TreeMap<String,TreeMap<Integer, Integer>>();
	//HashMap<Integer, Integer> histogram;// = new HashMap<Integer, Integer>();

	public FragmentLocator(){
		this.fragmentClasses = new ArrayList<String>();
		fragmentClasses.add(AndroidEntryPointConstants.V4FRAGMENTCLASS);
		fragmentClasses.add(AndroidEntryPointConstants.FRAGMENTCLASS);
	}
	
	/**
	 * Look at all classes and analyze them
	 */
	public void collectFragments(){
		this.entrypoints = new HashSet<String>();

		Chain<SootClass> classes = Scene.v().getClasses();
		Iterator<SootClass> it = classes.iterator();
		while (it.hasNext()){
			SootClass sootClass = it.next();
			trackClassNameLength(sootClass);

			//Ignore android framework
			if (!sootClass.getName().startsWith("android") && hasSuperClass(sootClass, fragmentClasses)){
				logger.debug("Found fragment class {}", sootClass.getName());
				this.entrypoints.add(sootClass.getName());
			}
		}
		
		
		identifyObfuscatedPackages();
		
	}
	
	
	private void identifyObfuscatedPackages(){
		
		Iterator<String> it = classNameHistogram.keySet().iterator();
		while (it.hasNext()){
			String packageName = it.next();
			TreeMap<Integer, Integer> histogram = classNameHistogram.get(packageName);
			int count = getMostFrequentClassnameLength(histogram);
			if (count == 1){
				logger.info("Package {} is OBFUSCATED {}", packageName, histogram);
			} else {
				logger.info("Package {} {} ", packageName, histogram);
			}
		}
	}
	

	/**
	 * If the package has the majority of class names of length 1 then the package is obfuscated
	 * @param packageHistogram
	 * @return
	 */
	private int getMostFrequentClassnameLength(TreeMap<Integer, Integer> packageHistogram){
		Set<Integer> packageNames = packageHistogram.keySet();
		Iterator<Integer> it = packageNames.iterator();
		int maxClassOccurences = 0;
		int classNameLengthMaxOccurences = 0;
		while (it.hasNext()){
			Integer classNameLength = it.next();
			Integer occurence = packageHistogram.get(classNameLength);
			if (occurence > maxClassOccurences){
				maxClassOccurences = occurence;
				classNameLengthMaxOccurences = classNameLength;
			}
			logger.trace("{} classes have class names of {} length",occurence,classNameLength);
		}
		return classNameLengthMaxOccurences;
	}
	
	
	private void trackClassNameLength(SootClass sootClass){
		String packageName = sootClass.getPackageName();
		int lengthName = sootClass.getShortName().length();
		if (!classNameHistogram.containsKey(packageName)){
			TreeMap<Integer, Integer> histogram = new TreeMap<Integer, Integer>();
			classNameHistogram.put(packageName, histogram);
		}
		
		TreeMap<Integer, Integer> packageHistogram = classNameHistogram.get(packageName);
		
		
		Integer frequency = packageHistogram.get(lengthName);
		if (frequency == null){
			packageHistogram.put(lengthName, 1);
		} else {
			packageHistogram.put(lengthName, frequency + 1);
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
	}
	


}

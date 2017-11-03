/*
 * MATLAB Compiler: 5.1 (R2014a)
 * Date: Wed Apr 06 12:47:26 2016
 * Arguments: "-B" "macro_default" "-W" "java:Comparator,Class1" "-T" "link:lib" "-d" 
 * "C:\\Users\\aagraw25\\Documents\\MATLAB\\Comparator\\for_testing" "-v" 
 * "C:\\Users\\aagraw25\\workspace\\MatlabConnection\\MatlabConnection\\Comparator.m" 
 * "class{Class1:C:\\Users\\aagraw25\\workspace\\MatlabConnection\\MatlabConnection\\Comparator.m}" 
 */

package Comparator;

import com.mathworks.toolbox.javabuilder.*;
import com.mathworks.toolbox.javabuilder.internal.*;

/**
 * <i>INTERNAL USE ONLY</i>
 */
public class ComparatorMCRFactory
{
   
    
    /** Component's uuid */
    private static final String sComponentId = "Comparator_A3111D2168C369720F8FB8FF58C9ACC4";
    
    /** Component name */
    private static final String sComponentName = "Comparator";
    
   
    /** Pointer to default component options */
    private static final MWComponentOptions sDefaultComponentOptions = 
        new MWComponentOptions(
            MWCtfExtractLocation.EXTRACT_TO_CACHE, 
            new MWCtfClassLoaderSource(ComparatorMCRFactory.class)
        );
    
    
    private ComparatorMCRFactory()
    {
        // Never called.
    }
    
    public static MWMCR newInstance(MWComponentOptions componentOptions) throws MWException
    {
        if (null == componentOptions.getCtfSource()) {
            componentOptions = new MWComponentOptions(componentOptions);
            componentOptions.setCtfSource(sDefaultComponentOptions.getCtfSource());
        }
        return MWMCR.newInstance(
            componentOptions, 
            ComparatorMCRFactory.class, 
            sComponentName, 
            sComponentId,
            new int[]{8,3,0}
        );
    }
    
    public static MWMCR newInstance() throws MWException
    {
        return newInstance(sDefaultComponentOptions);
    }
}

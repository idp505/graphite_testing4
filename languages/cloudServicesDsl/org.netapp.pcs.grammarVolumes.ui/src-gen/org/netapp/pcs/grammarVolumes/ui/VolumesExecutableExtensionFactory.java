/*
 * generated by Xtext 2.36.0
 */
package org.netapp.pcs.grammarVolumes.ui;

import com.google.inject.Injector;
import org.eclipse.xtext.ui.guice.AbstractGuiceAwareExecutableExtensionFactory;
import org.netapp.pcs.grammarVolumes.ui.internal.GrammarVolumesActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This class was generated. Customizations should only happen in a newly
 * introduced subclass. 
 */
public class VolumesExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {

	@Override
	protected Bundle getBundle() {
		return FrameworkUtil.getBundle(GrammarVolumesActivator.class);
	}
	
	@Override
	protected Injector getInjector() {
		GrammarVolumesActivator activator = GrammarVolumesActivator.getInstance();
		return activator != null ? activator.getInjector(GrammarVolumesActivator.ORG_NETAPP_PCS_GRAMMARVOLUMES_VOLUMES) : null;
	}

}

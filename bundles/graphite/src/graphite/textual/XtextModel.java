package graphite.textual;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.epsilon.emc.emf.DefaultXMIResource;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.xtext.resource.XtextResourceSet;

public class XtextModel extends EmfModel {

    private Resource externalResource;

    public void setExternalResource(Resource resource) {
        this.externalResource = resource;
    }
    
    @Override
    protected ResourceSet createResourceSet() {
    	XtextResourceSet rs = new XtextResourceSet();
    	if (externalResource != null && externalResource.getResourceSet() != null) {
    		rs.getResources().addAll(externalResource.getResourceSet().getResources());
    	}
    	return rs;
    }
    
    
    /*
    @Override
    protected ResourceSet createResourceSet() {
        XtextResourceSet rs = new XtextResourceSet();

        if (externalResource != null) {
            ResourceSet externalRs = externalResource.getResourceSet();
            if (externalRs != null) {
                // Make a copy to avoid concurrent modification
                List<Resource> resourcesCopy = new ArrayList<>(externalRs.getResources());
                for (Resource r : resourcesCopy) {
                    // Only add the resource if not already present in rs by URI
                    if (rs.getResource(r.getURI(), false) == null) {
                        rs.getResources().add(r);
                    }
                }
            }
        }

        return rs;
    }
    */
    
    /*
    @Override
    public void loadModelFromUri() throws EolModelLoadingException {
        ResourceSet resourceSet = createResourceSet();

        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("model", new DefaultXMIResource.Factory());

        // Ensure EcorePackage is registered globally
        if (EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI) == null) {
            EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        }

        determinePackagesFrom(resourceSet);

        // Register packages in the resource set's package registry
        for (EPackage ep : packages) {
            String nsUri = ep.getNsURI();
            if (nsUri == null || nsUri.trim().isEmpty()) {
                nsUri = ep.getName();
            }
            resourceSet.getPackageRegistry().put(nsUri, ep);
        }
        resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

        resourceSet.getResources().clear();
        
        
        // Check if resource exists and unload + remove it to replace with a fresh one
        Resource existingResource = resourceSet.getResource(modelUri, false);
        if (existingResource != null) {
            existingResource.unload();
            resourceSet.getResources().remove(existingResource);
            
           // if (resourceSet instanceof XtextResourceSet) {
          //      ((XtextResourceSet) resourceSet).getURIResourceMap().remove(modelUri);
           // }
            
        }
         

        // Now create a fresh resource for the URI
        Resource newResource = resourceSet.createResource(modelUri);
        modelImpl = newResource;

        if (this.readOnLoad) {
            try {
                modelImpl.load(getResourceLoadOptions());
                if (expand) {
                    EcoreUtil.resolveAll(modelImpl);
                }
            } catch (IOException e) {
                disposeModel();
                throw new EolModelLoadingException(e, this);
            }
        }

        if (isCachingEnabled()) {
            addContentsAdapter();
        }
    }
    */
	
}
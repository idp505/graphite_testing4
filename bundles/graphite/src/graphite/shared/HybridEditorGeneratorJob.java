package graphite.shared;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.epsilon.common.dt.console.EpsilonConsole;
import org.eclipse.epsilon.egl.EgxModule;
import org.eclipse.epsilon.egl.execute.context.IEglContext;
import org.eclipse.epsilon.egl.status.StatusMessage;
import org.eclipse.epsilon.emc.emf.CachedResourceSet;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.plainxml.PlainXmlModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.eclipse.xtext.XtextPackage;
import org.eclipse.xtext.ui.MarkerTypes;
import org.eclipse.sirius.diagram.description.DescriptionPackage;
import org.eclipse.sirius.properties.PropertiesPackage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import graphite.textual.XtextModel;


public class HybridEditorGeneratorJob extends Job {

	private Map<String, List<IFile>> selectedModels;
	
	public HybridEditorGeneratorJob(Map<String, List<IFile>> selectedModels)  {
		super("Generate Sirius Web editor");
		this.selectedModels = selectedModels;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			boolean isValid = generateHybridEditor();
			return isValid ? Status.OK_STATUS : Status.CANCEL_STATUS;
		}
		catch (Exception e) {
			e.printStackTrace();
			return Status.CANCEL_STATUS;
		}
	}
	
	public boolean generateHybridEditor() throws Exception {
		
		EvlModule metamodelEvlModule = null;
		List<EvlModule> grammarEvlModules = null;
		EolModule extendOdesignEolModule = null;
		EolModule extendPluginEolModule = null;
		EgxModule egxModule = null;
		
		EpsilonConsole.getInstance().clear();

		try {
			
			if (selectedModels.get(ModelExtension.METAMODEL_EXTENSION).size() > 1) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
		 				"Generation Failed",
		 				"There must be a single metamodel (.ecore) selected.");
		 		});
				return false;
			}
			else if (selectedModels.get(ModelExtension.GENMODEL_EXTENSION).size() > 1) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
		 				"Generation Failed",
		 				"There must be a single generator model (.genmodel) selected.");
		 		});
				return false;
			}
			else if (selectedModels.get(ModelExtension.ODESIGN_EXTENSION).size() > 1) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
		 				"Generation Failed",
		 				"There must be a single Sirius VSM (.odesign) selected.");
		 		});
				return false;
			}
			
			IFile metamodelFile = selectedModels.get(ModelExtension.METAMODEL_EXTENSION).get(0);
			IFile genmodelFile = selectedModels.get(ModelExtension.GENMODEL_EXTENSION).get(0);
			IFile odesignFile = selectedModels.get(ModelExtension.ODESIGN_EXTENSION).get(0);
			List<IFile> grammarFiles = selectedModels.get(ModelExtension.GRAMMAR_EXTENSION);
			Map<IFile, URI> grammarUris = new HashMap<IFile, URI>();
			Map<IFile, XtextModel> xtextModels = new HashMap<IFile, XtextModel>();
			Map<IFile, Collection<UnsatisfiedConstraint>> grammarsUnsatisfiedConstraints = new HashMap<IFile, Collection<UnsatisfiedConstraint>>();
			Map<IFile, Collection<UnsatisfiedConstraint>> grammarsUnsatisfiedCritiques = new HashMap<IFile, Collection<UnsatisfiedConstraint>>();
			boolean grammarsHaveUnsatisfiedConstraints = false;
			boolean grammarsHaveUnsatisfiedCritiques = false;
			String emfProjectPath = HandlerUtilityService.getProjectPath(metamodelFile) + "/";
			StringBuilder grammarsParameter = new StringBuilder();
			
			metamodelFile.deleteMarkers(EValidator.MARKER, true, IResource.DEPTH_ZERO);
			
			URI metamodelUri = HandlerUtilityService.getPlatformURI(metamodelFile);
			EmfModel metamodel = new EmfModel();
	 		metamodel.setMetamodelUris(Arrays.asList(EcorePackage.eINSTANCE.getNsURI()));
	 		metamodel.setModelFileUri(metamodelUri);
	 		metamodel.setName("Metamodel");
	 		metamodel.setReadOnLoad(true);
	 		metamodel.setStoredOnDisposal(false);
	 		metamodel.setCachingEnabled(false);
	 		metamodel.load();
	 		
	 		EPackage mainPackage = (EPackage)metamodel.getResource().getContents().get(0);
	 		String mainPackagePrefix = (mainPackage.getNsPrefix() == null || mainPackage.getNsPrefix().isBlank()) ? mainPackage.getName() : mainPackage.getNsPrefix();
	 		String editorStartupClass = mainPackage.getName() + ".impl." + ModelUtility.firstToUpperCase(mainPackagePrefix) + "EditorStartup";		

	 		for (IFile grammarFile: grammarFiles) {
	 			grammarFile.deleteMarkers(MarkerTypes.FAST_VALIDATION, true, IResource.DEPTH_ZERO);

	 			URI grammarUri = HandlerUtilityService.getPlatformURI(grammarFile);
	 			grammarUris.put(grammarFile, grammarUri);
	 			
		 		XtextModel grammar = new XtextModel();
		 		grammar.setMetamodelUris(Arrays.asList(XtextPackage.eINSTANCE.getNsURI()));
		 		grammar.setModelFileUri(grammarUri);
		 		grammar.setExternalResource(metamodel.getResource());
		 		grammar.setName("Grammar");
		 		grammar.setReadOnLoad(true);
		 		grammar.setStoredOnDisposal(false);
		 		grammar.setCachingEnabled(false);
		 		grammar.load();
		 		xtextModels.put(grammarFile, grammar);
		 		
		 		if (grammarsParameter.length() > 0) {
		 			grammarsParameter.append(";");
		 		}
		 		grammarsParameter.append(ModelUtility.getGrammarName(grammar));
	 		}

	 		metamodelEvlModule = new EvlModule();
	 		metamodelEvlModule.parse(getClass().getResource("/epsilon/MetamodelValidator.evl").toURI());
	 		metamodelEvlModule.getContext().getModelRepository().addModel(metamodel);
	 		metamodelEvlModule.getContext().getFrameStack().put(Variable.createReadOnlyVariable("grammars", grammarsParameter.toString()));
	 		metamodelEvlModule.execute();
	 		Collection<UnsatisfiedConstraint> metamodelUnsatisfiedConstraints = metamodelEvlModule.getContext().getUnsatisfiedConstraints();
		
	 		if (metamodelUnsatisfiedConstraints.size() == 0) {
	 			
	 			grammarEvlModules = new ArrayList<EvlModule>();
	 			
	 			for (Map.Entry<IFile, XtextModel> entry : xtextModels.entrySet()) {
	 				IFile grammarFile = entry.getKey();
	 				XtextModel xtextModel = entry.getValue();
	 				
	 				EvlModule grammarEvlModule = new EvlModule();
					grammarEvlModule.parse(getClass().getResource("/epsilon/ValidateGrammar.evl").toURI());
					grammarEvlModule.getContext().getModelRepository().addModel(metamodel);
					grammarEvlModule.getContext().getModelRepository().addModel(xtextModel);
					grammarEvlModule.execute();
					grammarEvlModules.add(grammarEvlModule);
					
					Collection<UnsatisfiedConstraint> grammarAllUnsatisfiedConstraints = grammarEvlModule.getContext().getUnsatisfiedConstraints();
					Collection<UnsatisfiedConstraint> grammarUnsatisfiedConstraints = grammarAllUnsatisfiedConstraints.stream().filter(c -> !c.getConstraint().isCritique()).toList();
					Collection<UnsatisfiedConstraint> grammarUnsatisfiedCritiques = grammarAllUnsatisfiedConstraints.stream().filter(c -> c.getConstraint().isCritique()).toList();
		 		 	
					if (!grammarsHaveUnsatisfiedConstraints) {
						grammarsHaveUnsatisfiedConstraints = (grammarUnsatisfiedConstraints.size() > 0);
					}
					if (!grammarsHaveUnsatisfiedCritiques) {
						grammarsHaveUnsatisfiedCritiques = (grammarUnsatisfiedCritiques.size() > 0);
					}
					grammarsUnsatisfiedConstraints.put(grammarFile, grammarUnsatisfiedConstraints);
					grammarsUnsatisfiedCritiques.put(grammarFile, grammarUnsatisfiedCritiques);	
	 			}
	 			
	 			if (grammarsHaveUnsatisfiedCritiques) {
					for (Map.Entry<IFile, Collection<UnsatisfiedConstraint>> entry : grammarsUnsatisfiedCritiques.entrySet()) {
					    IFile grammarFile = entry.getKey();
					    Collection<UnsatisfiedConstraint> critiques = entry.getValue();
					    ErrorReportingUtility.createMarkers(critiques, MarkerTypes.FAST_VALIDATION, grammarFile, grammarUris.get(grammarFile));
					}
				}		
	 			
	 			if (!grammarsHaveUnsatisfiedConstraints) {
					
					URI genmodelUri = HandlerUtilityService.getPlatformURI(genmodelFile);
					EmfModel genmodel = new EmfModel();
			 		genmodel.setMetamodelUris(Arrays.asList(EcorePackage.eINSTANCE.getNsURI(), GenModelPackage.eINSTANCE.getNsURI()));
			 		genmodel.setModelFileUri(genmodelUri);
			 		genmodel.setName("Genmodel");
			 		genmodel.setReadOnLoad(true);
			 		genmodel.setStoredOnDisposal(false);
			 		genmodel.load();
			 		
			 		String editorProjectName = ((GenModel)genmodel.getResource().getContents().get(0)).getEditorPluginID();
			 		String metamodelProjectRelativePath = metamodelFile.getFullPath().toString();
			 		String metamodelProjectAbsolutePath = metamodelFile.getLocation().toString();
			 		String pluginXmlPath = metamodelProjectAbsolutePath.replace(metamodelProjectRelativePath, "/" + editorProjectName + "/plugin.xml");
			 		
					PlainXmlModel pluginXml = new PlainXmlModel();
					pluginXml.setFile(new File(pluginXmlPath));
					pluginXml.setName("Plugin");
					pluginXml.setReadOnLoad(true);
					pluginXml.setStoredOnDisposal(true);
					pluginXml.load();
			 			
			 		URI odesignUri = HandlerUtilityService.getPlatformURI(odesignFile);
			 		EmfModel odesign = new EmfModel();
			 		odesign.setMetamodelUris(Arrays.asList(DescriptionPackage.eINSTANCE.getNsURI(), PropertiesPackage.eINSTANCE.getNsURI()));
			 		odesign.setModelFileUri(odesignUri);
			 		odesign.setName("Odesign");
			 		odesign.setReadOnLoad(true);
			 		odesign.setStoredOnDisposal(true);
			 		odesign.load();
			 		
			 		URI importedOdesignUri = URI.createURI(getClass().getResource("/epsilon/graphite_properties.odesign").toString());
			 		EmfModel importedOdesign = new EmfModel();
			 		importedOdesign.setMetamodelUris(Arrays.asList(DescriptionPackage.eINSTANCE.getNsURI(), PropertiesPackage.eINSTANCE.getNsURI()));
			 		importedOdesign.setModelFileUri(importedOdesignUri);
			 		importedOdesign.setName("ImportedOdesign");
			 		importedOdesign.setReadOnLoad(true);
			 		importedOdesign.setStoredOnDisposal(false);
			 		importedOdesign.load();
			 		
			 		extendOdesignEolModule = new EolModule();
			 		extendOdesignEolModule.parse(getClass().getResource("/epsilon/ExtendOdesign.eol").toURI());
			 		extendOdesignEolModule.getContext().getModelRepository().addModel(importedOdesign);
			 		extendOdesignEolModule.getContext().getModelRepository().addModel(odesign);
			 		extendOdesignEolModule.execute();
			 		
			 		EpsilonConsole.getInstance().getInfoStream().println("Successfully patched " + odesignFile.getLocation().toString() + " with a Graphite-specific Properties View containing smart textual editors");
			 		
					extendPluginEolModule = new EolModule();
					extendPluginEolModule.parse(getClass().getResource("/epsilon/ExtendPlugin.eol").toURI());
					extendPluginEolModule.getContext().getModelRepository().addModel(pluginXml);
					extendPluginEolModule.getContext().getFrameStack().put(Variable.createReadOnlyVariable("startupClass", editorStartupClass));
					extendPluginEolModule.execute(); 				 		
			 		
					EpsilonConsole.getInstance().getInfoStream().println("Successfully patched " + pluginXmlPath + " with the 'org.eclipse.ui.startup' extension point");
			 		
					egxModule = new EgxModule(new File(".").getAbsolutePath());
			 		egxModule.parse(getClass().getResource("/epsilon/HybridEditorGenerator.egx").toURI());
			 		egxModule.getContext().getModelRepository().addModel(metamodel);
			 		egxModule.getContext().getModelRepository().addModel(genmodel);
			 		egxModule.getContext().getFrameStack().put(Variable.createReadOnlyVariable("emfProjectPath", emfProjectPath));
			 		egxModule.execute();
			 		
			 		IEglContext context = egxModule.getContext().getTemplateFactory().getContext();
					for (StatusMessage message : context.getStatusMessages()) {
						EpsilonConsole.getInstance().getInfoStream().println(message);
					}
					
			 		Display.getDefault().asyncExec(() -> {
			 			MessageDialog.openInformation(Display.getDefault().getActiveShell(),
			 				"Generation Success",
			 				"The hybrid editor has been generated successfully.");
			 		});
			        return true;
				}
				else {				
					for (Map.Entry<IFile, Collection<UnsatisfiedConstraint>> entry : grammarsUnsatisfiedConstraints.entrySet()) {
					    IFile grammarFile = entry.getKey();
					    Collection<UnsatisfiedConstraint> constraints = entry.getValue();
					    ErrorReportingUtility.createMarkers(constraints, MarkerTypes.FAST_VALIDATION, grammarFile, grammarUris.get(grammarFile));
					}
					Display.getDefault().asyncExec(() -> {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Generation Failed",
							"The grammar(s) is invalid. Please check the Console and Problems View for details.");
			        });
					return false;
				}	
	 			
	 		}
	 		else {
				ErrorReportingUtility.createMarkers(metamodelUnsatisfiedConstraints, EValidator.MARKER, null, null);
	 			Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Generation Failed",
						"The metamodel is invalid. Please check the Console and Problems View for details.");
		        });
				return false;
	 		}	
			
		}
		catch (Exception e) {
			Display.getDefault().asyncExec(() -> {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
	 				"Generation Failed",
	 				"An error occured during the code generation operation. Please check the Console and Problems View for details.");
	 		});
			EpsilonConsole.getInstance().getErrorStream().println(e.getMessage());
	        return false;
		}
		finally {
			if (metamodelEvlModule != null) {
				metamodelEvlModule.getContext().getModelRepository().dispose();
			}
			if (extendOdesignEolModule != null) {
				extendOdesignEolModule.getContext().getModelRepository().dispose();
			}
			if (extendPluginEolModule != null) {
				extendPluginEolModule.getContext().getModelRepository().dispose();
			}
			if (egxModule != null) {
				egxModule.getContext().getModelRepository().dispose();
			}
			if (grammarEvlModules != null) {
				for (EvlModule grammarEvlModule : grammarEvlModules) {
					grammarEvlModule.getContext().getModelRepository().dispose();
				}
			}
			CachedResourceSet.getCache().clear();
		}
	}	
	
}

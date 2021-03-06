package net.heartsome.cat.ts.ui.preferencepage;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.internal.keys.KeysPreferencePage;
import org.eclipse.ui.internal.keys.NewKeysPreferenceMessages;
import org.eclipse.ui.internal.keys.model.BindingElement;
import org.eclipse.ui.internal.keys.model.CommonModel;
import org.eclipse.ui.internal.keys.model.ContextElement;
import org.eclipse.ui.internal.keys.model.ContextModel;
import org.eclipse.ui.internal.keys.model.KeyController;
import org.eclipse.ui.internal.keys.model.ModelElement;
import org.eclipse.ui.internal.keys.model.SchemeElement;
import org.eclipse.ui.internal.keys.model.SchemeModel;
import org.eclipse.ui.internal.misc.StatusUtil;
import org.eclipse.ui.internal.util.Util;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * 修改了 KeyController 中的 init 和 setDefaultBindings 方法
 * @author  peason
 * @version 
 * @since   JDK1.6
 */
public class KeyController2 extends KeyController {

	private static final String DELIMITER = ","; //$NON-NLS-1$
	private static final String ESCAPED_QUOTE = "\""; //$NON-NLS-1$
	private static final String REPLACEMENT = "\"\""; //$NON-NLS-1$
	/**
	 * The resource bundle from which translations can be retrieved.
	 */
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(KeysPreferencePage.class.getName());
	private ListenerList eventManager = null;
	private BindingManager fBindingManager;
	private ContextModel contextModel;
	private SchemeModel fSchemeModel;
	private BindingModel2 bindingModel;
	private boolean notifying = true;
	private ConflictModel2 conflictModel;
	private IServiceLocator serviceLocator;
	private ListenerList getEventManager() {
		if (eventManager == null) {
			eventManager = new ListenerList(ListenerList.IDENTITY);
		}
		return eventManager;
	}

	public void setNotifying(boolean b) {
		notifying = b;
	}

	public boolean isNotifying() {
		return notifying;
	}

	public void firePropertyChange(Object source, String propId, Object oldVal, Object newVal) {
		if (!isNotifying()) {
			return;
		}
		if (Util.equals(oldVal, newVal)) {
			return;
		}

		Object[] listeners = getEventManager().getListeners();
		PropertyChangeEvent event = new PropertyChangeEvent(source, propId, oldVal, newVal);
		for (int i = 0; i < listeners.length; i++) {
			((IPropertyChangeListener) listeners[i]).propertyChange(event);
		}
	}

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		getEventManager().add(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		getEventManager().remove(listener);
	}

	public void init(IServiceLocator locator, List<String> lstRemove) {
		getEventManager().clear();
		this.serviceLocator = locator;
	//	filterDupliteBind();
		fBindingManager = loadModelBackend(serviceLocator);
		contextModel = new ContextModel(this);
		contextModel.init(serviceLocator);
		fSchemeModel = new SchemeModel(this);
		fSchemeModel.init(fBindingManager);
		bindingModel = new BindingModel2(this);
		bindingModel.init(serviceLocator, fBindingManager, contextModel);

		HashSet<BindingElement> set = bindingModel.getBindings();
		Iterator<BindingElement> iterator = set.iterator();
		while (iterator.hasNext()) {
			BindingElement bindingElement = iterator.next();
			if (lstRemove.contains(bindingElement.getId())) {
				iterator.remove();
			}
		}
		bindingModel.setBindings(set);
		Map<Binding, BindingElement> mapBBe = bindingModel.getBindingToElement();
		Iterator<Entry<Binding, BindingElement>> it = mapBBe.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Binding, BindingElement> entry = (Entry<Binding, BindingElement>) it.next();
			if (lstRemove.contains(entry.getValue().getId())) {
				it.remove();
			}
		}
		bindingModel.setBindingToElement(mapBBe);

		conflictModel = new ConflictModel2(this);
		conflictModel.init(fBindingManager, bindingModel);
		addSetContextListener();
		addSetBindingListener();
		addSetConflictListener();
		addSetKeySequenceListener();
		addSetSchemeListener();
		addSetModelObjectListener();
	}

	@SuppressWarnings("restriction")
	public void filterDupliteBind(){
		IWorkbench workbench = PlatformUI.getWorkbench();
		IBindingService bindingService = (IBindingService) workbench.getService(IBindingService.class);
		BindingService service =(BindingService)bindingService;
		BindingManager bindingManager = service.getBindingManager();
		//service.getBindingManager().
		Binding[] bindings = bindingManager.getBindings();
	     List<Binding> bindTemp = new ArrayList<Binding>();
	     List<String> ids = new ArrayList<String>();
	     for(Binding bind : bindings){
			if(null ==bind){
				continue;
			}
			ParameterizedCommand command = bind.getParameterizedCommand();
			if(null == command){
				continue;
			}
			String id = command.getId();
			if(!ids.contains(id)){
				ids.add(id);
				bindTemp.add(bind);
			}
		}
	   
	     bindingManager.setBindings(bindTemp.toArray(new Binding[ids.size()]));
	}
	
	private static BindingManager loadModelBackend(IServiceLocator locator) {
		IBindingService bindingService = (IBindingService) locator.getService(IBindingService.class);
		BindingManager bindingManager = new BindingManager(new ContextManager(), new CommandManager());
		final Scheme[] definedSchemes = bindingService.getDefinedSchemes();
		try {
			Scheme modelActiveScheme = null;
			for (int i = 0; i < definedSchemes.length; i++) {
				final Scheme scheme = definedSchemes[i];
				final Scheme copy = bindingManager.getScheme(scheme.getId());
				copy.define(scheme.getName(), scheme.getDescription(), scheme.getParentId());
				if (definedSchemes[i] == bindingService.getActiveScheme()) {
					modelActiveScheme = copy;
				}
			}
			bindingManager.setActiveScheme(modelActiveScheme);
		} catch (final NotDefinedException e) {
			StatusManager.getManager()
					.handle(new Status(IStatus.WARNING, WorkbenchPlugin.PI_WORKBENCH,
							"Keys page found an undefined scheme", e)); //$NON-NLS-1$
		}
		bindingManager.setLocale(bindingService.getLocale());
		bindingManager.setPlatform(bindingService.getPlatform());
		bindingManager.setBindings(bindingService.getBindings());
		return bindingManager;
	}

	public ContextModel getContextModel() {
		return contextModel;
	}

	public SchemeModel getSchemeModel() {
		return fSchemeModel;
	}

	public BindingModel2 getBindingModel() {
		return bindingModel;
	}

	public ConflictModel2 getConflictModel() {
		return conflictModel;
	}

	private void addSetContextListener() {
		addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getSource() == contextModel && CommonModel.PROP_SELECTED_ELEMENT.equals(event.getProperty())) {
					updateBindingContext((ContextElement) event.getNewValue());
				}
			}
		});
	}

	private void addSetBindingListener() {
		addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getSource() == bindingModel && CommonModel.PROP_SELECTED_ELEMENT.equals(event.getProperty())) {
					BindingElement binding = (BindingElement) event.getNewValue();
//					Bug #2740
					if (binding == null || binding.getTrigger() == null) {
						conflictModel.setSelectedElement(null);
						return;
					}
					conflictModel.setSelectedElement(binding);
					ContextElement context = binding.getContext();
					if (context != null) {
						contextModel.setSelectedElement(context);
					}
				}
			}
		});
	}

	private void addSetConflictListener() {
		addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getSource() == conflictModel && CommonModel.PROP_SELECTED_ELEMENT.equals(event.getProperty())) {
					if (event.getNewValue() != null) {
						bindingModel.setSelectedElement((ModelElement) event.getNewValue());
					}
				}
			}
		});
	}

	private void addSetKeySequenceListener() {
		addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (BindingElement.PROP_TRIGGER.equals(event.getProperty())) {
//					Bug #2740
					if (((KeySequence) event.getNewValue()) != null) {
						if (((KeySequence) event.getNewValue()).toString().endsWith("+")) {
							return;
						} else {
							updateTrigger((BindingElement) event.getSource(), (KeySequence) event.getOldValue(),
									(KeySequence) event.getNewValue());
						}
					}
				}
			}
		});
	}

	private void addSetModelObjectListener() {
		addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getSource() instanceof BindingElement
						&& ModelElement.PROP_MODEL_OBJECT.equals(event.getProperty())) {
					if (event.getNewValue() != null) {
						BindingElement element = (BindingElement) event.getSource();
						Object oldValue = event.getOldValue();
						Object newValue = event.getNewValue();
//						Bug #2740 快捷键--快捷键设置问题
						if (oldValue instanceof Binding && newValue instanceof Binding) {
							conflictModel.updateConflictsFor(element, ((Binding) oldValue).getTriggerSequence(),
									((Binding) newValue).getTriggerSequence(), false);
						} else if (oldValue instanceof Binding) {
							conflictModel.updateConflictsFor(element, ((Binding) oldValue).getTriggerSequence(),
									null, false);
						} else if (newValue instanceof Binding) {
							conflictModel.updateConflictsFor(element, null,
									((Binding) newValue).getTriggerSequence(), false);
						} else {
							conflictModel.updateConflictsFor(element, false);
						}

						ContextElement context = element.getContext();
						if (context != null) {
							contextModel.setSelectedElement(context);
						}
					}
				}
			}
		});
	}

	private void addSetSchemeListener() {
		addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getSource() == fSchemeModel && CommonModel.PROP_SELECTED_ELEMENT.equals(event.getProperty())) {
					changeScheme((SchemeElement) event.getOldValue(), (SchemeElement) event.getNewValue());
				}
			}
		});
	}

	/**
	 * @param oldScheme
	 * @param newScheme
	 */
	protected void changeScheme(SchemeElement oldScheme, SchemeElement newScheme) {
		if (newScheme == null || newScheme.getModelObject() == fBindingManager.getActiveScheme()) {
			return;
		}
		try {
			fBindingManager.setActiveScheme((Scheme) newScheme.getModelObject());
			bindingModel.refresh(contextModel);
			bindingModel.setSelectedElement(null);
		} catch (NotDefinedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void updateBindingContext(ContextElement context) {
		if (context == null) {
			return;
		}
		BindingElement activeBinding = (BindingElement) bindingModel.getSelectedElement();
		if (activeBinding == null) {
			return;
		}
		String activeSchemeId = fSchemeModel.getSelectedElement().getId();
		Object obj = activeBinding.getModelObject();
		if (obj instanceof KeyBinding) {
			KeyBinding keyBinding = (KeyBinding) obj;
			if (!keyBinding.getContextId().equals(context.getId())) {
				final KeyBinding binding = new KeyBinding(keyBinding.getKeySequence(),
						keyBinding.getParameterizedCommand(), activeSchemeId, context.getId(), null, null, null,
						Binding.USER);
				if (keyBinding.getType() == Binding.USER) {
					fBindingManager.removeBinding(keyBinding);
				} else {
					fBindingManager.addBinding(new KeyBinding(keyBinding.getKeySequence(), null, keyBinding
							.getSchemeId(), keyBinding.getContextId(), null, null, null, Binding.USER));
				}
				bindingModel.getBindingToElement().remove(activeBinding.getModelObject());

				fBindingManager.addBinding(binding);
				activeBinding.fill(binding, contextModel);
				bindingModel.getBindingToElement().put(binding, activeBinding);
			}
		}
	}

	/**
	 * @param activeBinding
	 * @param oldSequence
	 * @param keySequence
	 */
	public void updateTrigger(BindingElement activeBinding, KeySequence oldSequence, KeySequence keySequence) {
		if (activeBinding == null) {
			return;
		}
		Object obj = activeBinding.getModelObject();
		if (obj instanceof KeyBinding) {
			KeyBinding keyBinding = (KeyBinding) obj;
			if (!keyBinding.getKeySequence().equals(keySequence)) {
				if (keySequence != null && !keySequence.isEmpty()) {
					String activeSchemeId = fSchemeModel.getSelectedElement().getId();
					ModelElement selectedElement = contextModel.getSelectedElement();
					String activeContextId = selectedElement == null ? IContextService.CONTEXT_ID_WINDOW
							: selectedElement.getId();
					final KeyBinding binding = new KeyBinding(keySequence, keyBinding.getParameterizedCommand(),
							activeSchemeId, activeContextId, null, null, null, Binding.USER);
					Map bindingToElement = bindingModel.getBindingToElement();
					bindingToElement.remove(keyBinding);
					if (keyBinding.getType() == Binding.USER) {
						fBindingManager.removeBinding(keyBinding);
					} else {
						fBindingManager.addBinding(new KeyBinding(keyBinding.getKeySequence(), null, keyBinding
								.getSchemeId(), keyBinding.getContextId(), null, null, null, Binding.USER));
					}

					fBindingManager.addBinding(binding);
					activeBinding.fill(binding, contextModel);
					bindingModel.getBindingToElement().put(binding, activeBinding);

					// Remove binding for any system conflicts
					Object[] keys = bindingToElement.keySet().toArray();
					for (int i = 0; i < keys.length; i++) {
						Binding bindingKey = (Binding) keys[i];
						if (oldSequence.equals(bindingKey.getTriggerSequence())
								&& bindingKey.getType() == Binding.SYSTEM) {
							BindingElement be = (BindingElement) bindingToElement.get(bindingKey);
							bindingModel.remove(be);
						}
					}

					bindingModel.setSelectedElement(activeBinding);
				} else {
					bindingModel.getBindingToElement().remove(keyBinding);
					if (keyBinding.getType() == Binding.USER) {
						fBindingManager.removeBinding(keyBinding);
					} else {
						fBindingManager.addBinding(new KeyBinding(keyBinding.getKeySequence(), null, keyBinding
								.getSchemeId(), keyBinding.getContextId(), null, null, null, Binding.USER));
					}
					activeBinding.fill(keyBinding.getParameterizedCommand());
				}
			}
		} else if (obj instanceof ParameterizedCommand) {
			ParameterizedCommand cmd = (ParameterizedCommand) obj;
			if (keySequence != null && !keySequence.isEmpty()) {
				String activeSchemeId = fSchemeModel.getSelectedElement().getId();
				ModelElement selectedElement = contextModel.getSelectedElement();
				String activeContextId = selectedElement == null ? IContextService.CONTEXT_ID_WINDOW : selectedElement
						.getId();
				final KeyBinding binding = new KeyBinding(keySequence, cmd, activeSchemeId, activeContextId, null,
						null, null, Binding.USER);
				fBindingManager.addBinding(binding);
				activeBinding.fill(binding, contextModel);
				bindingModel.getBindingToElement().put(binding, activeBinding);
			}
		}
	}

	/**
	 * Replaces all the current bindings with the bindings in the local copy of the binding manager.
	 * @param bindingService
	 *            The binding service that saves the changes made to the local copy of the binding manager
	 */
	public void saveBindings(IBindingService bindingService) {
		try {
			bindingService.savePreferences(fBindingManager.getActiveScheme(), fBindingManager.getBindings());
		} catch (IOException e) {
			logPreferenceStoreException(e);
		}
	}

	/**
	 * Logs the given exception, and opens an error dialog saying that something went wrong. The exception is assumed to
	 * have something to do with the preference store.
	 * @param exception
	 *            The exception to be logged; must not be <code>null</code>.
	 */
	private final void logPreferenceStoreException(final Throwable exception) {
		final String message = NewKeysPreferenceMessages.PreferenceStoreError_Message;
		String exceptionMessage = exception.getMessage();
		if (exceptionMessage == null) {
			exceptionMessage = message;
		}
		final IStatus status = new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 0, exceptionMessage, exception);
		WorkbenchPlugin.log(message, status);
		StatusUtil.handleStatus(message, exception, StatusManager.SHOW);
	}

	/**
	 * Filters contexts for the When Combo.
	 * @param actionSets
	 *            <code>true</code> to filter action set contexts
	 * @param internal
	 *            <code>true</code> to filter internal contexts
	 * @param workbenchMenu
	 *            <code>true</code> to filter Workbench Menu Context
	 */
	public void filterContexts(boolean actionSets, boolean internal, boolean workbenchMenu) {
		contextModel.filterContexts(actionSets, internal, workbenchMenu);
	}

	/**
	 * Sets the bindings to default.
	 * @param bindingService
	 * @throws NotDefinedException
	 */
	public void setDefaultBindings(IBindingService bindingService, List<String> lstRemove) throws NotDefinedException {
		// Fix the scheme in the local changes.
		final String defaultSchemeId = bindingService.getDefaultSchemeId();
		final Scheme defaultScheme = fBindingManager.getScheme(defaultSchemeId);
		try {
			fBindingManager.setActiveScheme(defaultScheme);
		} catch (final NotDefinedException e) {
			// At least we tried....
		}

		// Restore any User defined bindings
		Binding[] bindings = fBindingManager.getBindings();
		for (int i = 0; i < bindings.length; i++) {
			ParameterizedCommand pCommand = bindings[i].getParameterizedCommand();
			String commandId = null;
			if (pCommand != null) {
				commandId = pCommand.getCommand().getId();
			}
			if (bindings[i].getType() == Binding.USER || (commandId != null && lstRemove.contains(commandId))) {
				fBindingManager.removeBinding(bindings[i]);
			}
		}
		bindingModel.refresh(contextModel, lstRemove);
		saveBindings(bindingService);
	}

	public void exportCSV(Shell shell) {
		final FileDialog fileDialog = new FileDialog(shell, SWT.SAVE | SWT.SHEET);
		fileDialog.setFilterExtensions(new String[] { "*.csv" }); //$NON-NLS-1$
		fileDialog.setFilterNames(new String[] { Util.translateString(RESOURCE_BUNDLE, "csvFilterName") }); //$NON-NLS-1$
		fileDialog.setOverwrite(true);
		final String filePath = fileDialog.open();
		if (filePath == null) {
			return;
		}

		final SafeRunnable runnable = new SafeRunnable() {
			public final void run() throws IOException {
				Writer fileWriter = null;
				try {
					fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")); //$NON-NLS-1$
					final Object[] bindingElements = bindingModel.getBindings().toArray();
					for (int i = 0; i < bindingElements.length; i++) {
						final BindingElement be = (BindingElement) bindingElements[i];
						if (be.getTrigger() == null || be.getTrigger().isEmpty() || be.getContext() == null
								|| be.getContext().getName() == null) {
							continue;
						}
						StringBuffer buffer = new StringBuffer();
						buffer.append(ESCAPED_QUOTE + Util.replaceAll(be.getCategory(), ESCAPED_QUOTE, REPLACEMENT)
								+ ESCAPED_QUOTE + DELIMITER);
						buffer.append(ESCAPED_QUOTE + be.getName() + ESCAPED_QUOTE + DELIMITER);
						buffer.append(ESCAPED_QUOTE + be.getTrigger().format() + ESCAPED_QUOTE + DELIMITER);
						buffer.append(ESCAPED_QUOTE + be.getContext().getName() + ESCAPED_QUOTE);
						buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
						fileWriter.write(buffer.toString());
					}

				} finally {
					if (fileWriter != null) {
						try {
							fileWriter.close();
						} catch (final IOException e) {
							// At least I tried.
						}
					}

				}
			}
		};
		SafeRunner.run(runnable);
	}

}

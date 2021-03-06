package net.heartsome.cat.ts.ui.rtf.handler;

import java.util.ArrayList;
import java.util.List;

import net.heartsome.cat.common.file.XLFValidator;
import net.heartsome.cat.common.resources.ResourceUtils;
import net.heartsome.cat.common.util.CommonFunction;
import net.heartsome.cat.ts.ui.editors.IXliffEditor;
import net.heartsome.cat.ts.ui.rtf.dialog.ExportRTFDilaog;
import net.heartsome.cat.ts.ui.rtf.resource.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.LoggerFactory;

/**
 * 导出 RTF 文件的 Handler
 * @author peason
 * @version
 * @since JDK1.6
 */
public class ExportRTFHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String tshelp = System.getProperties().getProperty("TSHelp");
		String tsstate = System.getProperties().getProperty("TSState");
		if (tshelp == null || !"true".equals(tshelp) || tsstate == null || !"true".equals(tsstate)) {
			LoggerFactory.getLogger(ExportRTFHandler.class).error("Exception:key hs008 is lost.(Can't find the key)");
			System.exit(0);
		}
		Shell shell = HandlerUtil.getActiveShell(event);
		String partId = HandlerUtil.getActivePartId(event);
		IFile file = null;
		if (partId.equals("net.heartsome.cat.common.ui.navigator.view")) {
			// 导航视图处于激活状态
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IViewPart viewPart = page.findView("net.heartsome.cat.common.ui.navigator.view");
			StructuredSelection selection = (StructuredSelection) viewPart.getSite().getSelectionProvider()
					.getSelection();
			// ISelection selection = HandlerUtil.getCurrentSelection(event);
			if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
				List<?> lstObj = ((IStructuredSelection) selection).toList();
				ArrayList<IFile> lstXliff = new ArrayList<IFile>();
				for (Object obj : lstObj) {
					if (obj instanceof IFile) {
						IFile tempFile = (IFile) obj;
						// Linux 下的文本文件无扩展名，因此要先判断扩展名是否为空
						if (tempFile.getFileExtension() != null && CommonFunction.validXlfExtension(tempFile.getFileExtension())) {
							lstXliff.add(tempFile);
						}
					}
				}
				if (lstXliff.size() > 1) {
					MessageDialog.openInformation(shell, Messages.getString("handler.ExportRTFHandler.msg.title"),
							Messages.getString("handler.ExportRTFHandler.msg1"));
					return null;
				}
				if (lstXliff.size() == 1) {
					file = lstXliff.get(0);
				}
			}
		} else if (partId.equals("net.heartsome.cat.ts.ui.xliffeditor.nattable.editor")) {
			// nattable 处于激活状态
			IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
			IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
			IFile iFile = (IFile) editorInput.getAdapter(IFile.class);
			IEditorPart editor = HandlerUtil.getActiveEditor(event);
			IXliffEditor xliffEditor = (IXliffEditor) editor;

			if (xliffEditor.isMultiFile()) {
				MessageDialog.openInformation(shell, Messages.getString("handler.ExportRTFHandler.msg.title"),
						Messages.getString("handler.ExportRTFHandler.msg2"));
				return null;
			} else if (iFile.getFileExtension() != null && CommonFunction.validXlfExtension(iFile.getFileExtension())) {
				file = iFile;
			}
		}

		if (file != null) {
			XLFValidator.resetFlag();
			if (!XLFValidator.validateXliffFile(file)) {
				return null;
			}
			XLFValidator.resetFlag();
		}

		ExportRTFDilaog dialog = new ExportRTFDilaog(shell, file == null ? "" : file.getFullPath().toOSString(),
				file == null ? "" : ResourceUtils.iFileToOSPath(file));
		dialog.open();
		return null;
	}
}

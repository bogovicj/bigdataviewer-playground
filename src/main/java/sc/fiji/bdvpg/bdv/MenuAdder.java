package sc.fiji.bdvpg.bdv;

import bdv.BigDataViewer;
//import bdv.util.BdvHandleFrame;

import javax.swing.*;
import java.awt.event.ActionListener;

public class MenuAdder
{
	private final BigDataViewer bdvHandle;
	private final ActionListener actionListener;

	public MenuAdder(BigDataViewer bdvHandle, ActionListener actionListener )
	{
		System.setProperty("apple.laf.useScreenMenuBar", "false");

		this.bdvHandle = bdvHandle;
		this.actionListener = actionListener;
	}

	public void addMenu( String menuText, String menuItemText )
	{
		/*
		final JMenu jMenu = createMenuItem( menuText, menuItemText );
		final JMenuBar bdvMenuBar = ( ( BdvHandleFrame ) bdvHandle ).getBigDataViewer().getViewerFrame().getJMenuBar();
		bdvMenuBar.add( jMenu );
		bdvMenuBar.updateUI();
		*/
	}

	public JMenu createMenuItem( String menuText, String menuItemText )
	{
		final JMenu jMenu = new JMenu( menuText );
		final JMenuItem jMenuItem = new JMenuItem( menuItemText );
		jMenuItem.addActionListener( actionListener );
		jMenu.add( jMenuItem );
		return jMenu;
	}
}

/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/sitemap/client/hoverbar/Attic/CmsHoverbarContextMenuButton.java,v $
 * Date   : $Date: 2010/11/18 15:39:34 $
 * Version: $Revision: 1.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.sitemap.client.hoverbar;

import org.opencms.ade.sitemap.client.ui.css.I_CmsImageBundle;
import org.opencms.gwt.client.ui.CmsContextMenu;
import org.opencms.gwt.client.ui.CmsContextMenuHandler;
import org.opencms.gwt.client.ui.CmsMenuButton;
import org.opencms.gwt.client.ui.I_CmsButton.Size;
import org.opencms.gwt.client.ui.I_CmsContextMenuEntry;
import org.opencms.gwt.client.ui.css.I_CmsLayoutBundle;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * Sitemap context menu button.<p>
 * 
 * @author Tobias Herrmann
 * 
 * @version $Revision: 1.3 $
 * 
 * @since 8.0.0
 */
public class CmsHoverbarContextMenuButton extends CmsMenuButton {

    /** The main content widget. */
    private FlexTable m_menuPanel;

    /** The context menu entries. */
    private List<I_CmsContextMenuEntry> m_entries;

    /** The width of the context menu popup. */
    protected static final int POPUP_WIDTH = 200;

    /**
     * Constructor.<p>
     * 
     * @param hoverbar the hoverbar 
     */
    public CmsHoverbarContextMenuButton(final CmsSitemapHoverbar hoverbar) {

        super(null, I_CmsImageBundle.INSTANCE.buttonCss().hoverbarContext());
        getElement().getStyle().setMarginTop(-3, Unit.PX);
        // create the menu panel (it's a table because of ie6)
        m_menuPanel = new FlexTable();
        // set a style name for the menu table
        m_menuPanel.getElement().addClassName(I_CmsLayoutBundle.INSTANCE.contextmenuCss().menuPanel());
        m_button.setSize(Size.small);
        // set the widget
        setMenuWidget(m_menuPanel);
        //    getPopupContent().removeAutoHidePartner(getElement());
        getPopupContent().addAutoHidePartner(getElement());
        getPopupContent().setWidth(POPUP_WIDTH + "px");
        //getPopupContent().setModal(true);
        m_entries = new ArrayList<I_CmsContextMenuEntry>();
        m_entries.add(new CmsGotoMenuEntry(hoverbar));
        m_entries.add(new CmsEditMenuEntry(hoverbar));
        m_entries.add(new CmsNewMenuEntry(hoverbar));
        m_entries.add(new CmsSubSitemapMenuEntry(hoverbar));
        m_entries.add(new CmsParentSitemapMenuEntry(hoverbar));
        m_entries.add(new CmsGotoSubSitemapMenuEntry(hoverbar));
        m_entries.add(new CmsMergeMenuEntry(hoverbar));
        m_entries.add(new CmsDeleteMenuEntry(hoverbar));
        setTitle("Context menu");
        setVisible(true);
        addClickHandler(new ClickHandler() {

            /**
             * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
             */
            public void onClick(ClickEvent event) {

                if (!isOpen()) {
                    showMenu(hoverbar);
                } else {
                    closeMenu();
                }
            }
        });
    }

    /**
     * Shows the context menu.<p>
     * 
     * @param hoverbar the hoverbar instance
     */
    protected void showMenu(final CmsSitemapHoverbar hoverbar) {

        // lock the hoverbar visibility to avoid hide on mouse out
        hoverbar.setLocked(true);
        CmsContextMenu menu = new CmsContextMenu(m_entries, false, getPopupContent());
        m_menuPanel.setWidget(0, 0, menu);
        // add the close handler for the menu
        getPopupContent().addCloseHandler(new CmsContextMenuHandler(menu));
        getPopupContent().addCloseHandler(new CloseHandler<PopupPanel>() {

            public void onClose(CloseEvent<PopupPanel> closeEvent) {

                onMenuClose(hoverbar);
            }
        });
        openMenu();
    }

    /**
     * Rests the button state and hides the hoverbar.<p>
     * 
     * @param hoverbar the hoverbar
     */
    protected void onMenuClose(CmsSitemapHoverbar hoverbar) {

        m_button.setDown(false);
        if (!hoverbar.isHovered()) {
            hoverbar.hide();
        } else {
            hoverbar.setLocked(false);
        }
    }
}

/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.planner.taskedition;

import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.planner.allocation.ResourceAllocationController;
import org.navalplanner.web.planner.order.SubcontractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.api.Tab;
import org.zkoss.zul.api.Tabbox;
import org.zkoss.zul.api.Tabpanel;
import org.zkoss.zul.api.Window;

/**
 * Controller for edit a {@link Task}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@org.springframework.stereotype.Component("editTaskController")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EditTaskController extends GenericForwardComposer {

    @Autowired
    private TaskPropertiesController taskPropertiesController;

    @Autowired
    private ResourceAllocationController resourceAllocationController;

    @Autowired
    private SubcontractController subcontractController;

    private Window window;

    private Tabbox editTaskTabbox;
    private Tab resourceAllocationTab;
    private Tab subcontractTab;
    private Tabpanel taskPropertiesTabpanel;
    private Tabpanel subcontractTabpanel;
    private Component messagesContainer;

    private IMessagesForUser messagesForUser;

    private TaskElement taskElement;

    private IContextWithPlannerTask<TaskElement> context;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        window = (Window) comp;
        taskPropertiesController.doAfterCompose(taskPropertiesTabpanel);
        subcontractController.doAfterCompose(subcontractTabpanel);
        messagesForUser = new MessagesForUser(messagesContainer);
    }

    public TaskPropertiesController getTaskPropertiesController() {
        return taskPropertiesController;
    }

    public ResourceAllocationController getResourceAllocationController() {
        return resourceAllocationController;
    }

    public SubcontractController getSubcontractController() {
        return subcontractController;
    }

    private void showEditForm(IContextWithPlannerTask<TaskElement> context,
            TaskElement taskElement) {
        this.taskElement = taskElement;
        this.context = context;

        taskPropertiesController.init(context, taskElement);
        if (taskElement.isSubcontracted()) {
            subcontractController.init((Task) taskElement, context);
        }

        try {
            Util.reloadBindings(window);
            window.setMode("modal");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void showEditFormTaskProperties(
            IContextWithPlannerTask<TaskElement> context,
            TaskElement taskElement) {
        editTaskTabbox.setSelectedPanelApi(taskPropertiesTabpanel);
        showEditForm(context, taskElement);
    }

    public void showEditFormSubcontract(
            IContextWithPlannerTask<TaskElement> context,
            TaskElement taskElement) {
        if (taskElement.isSubcontracted()) {
            editTaskTabbox.setSelectedPanelApi(subcontractTabpanel);
        }
        showEditForm(context, taskElement);
    }

    public void accept() {
        try {
            taskPropertiesController.accept();
            subcontractController.accept();

            taskElement = null;
            context = null;

            window.setVisible(false);
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
        }
    }

    public void cancel() {
        taskPropertiesController.cancel();
        subcontractController.cancel();

        taskElement = null;
        context = null;

        window.setVisible(false);
    }

    public void subcontract(boolean subcontract) {
        if (taskElement instanceof Task) {
            if (subcontract) {
                resourceAllocationTab.setVisible(false);
                subcontractTab.setVisible(true);
                subcontractController.init((Task) taskElement, context);
            } else {
                subcontractTab.setVisible(false);
                resourceAllocationTab.setVisible(true);
                subcontractController.removeSubcontractedTaskData();
            }
        }
    }

    public boolean isSubcontracted() {
        if (taskElement == null) {
            return false;
        }
        return taskElement.isSubcontracted();
    }

    public boolean isNotSubcontracted() {
        return !isSubcontracted();
    }

}
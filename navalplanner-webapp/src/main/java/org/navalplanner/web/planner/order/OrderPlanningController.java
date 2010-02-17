/*
 * This file is part of NavalPlan
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

package org.navalplanner.web.planner.order;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.web.common.ViewSwitcher;
import org.navalplanner.web.common.components.bandboxsearch.BandboxMultipleSearch;
import org.navalplanner.web.common.components.finders.FilterPair;
import org.navalplanner.web.orders.OrderElementPredicate;
import org.navalplanner.web.planner.allocation.ResourceAllocationController;
import org.navalplanner.web.planner.calendar.CalendarAllocationController;
import org.navalplanner.web.planner.taskedition.EditTaskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zkoss.ganttz.FilterAndParentExpandedPredicates;
import org.zkoss.ganttz.Planner;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.extensions.ICommand;
import org.zkoss.ganttz.extensions.IContext;
import org.zkoss.ganttz.resourceload.ScriptsRequiredByResourceLoadPanel;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.OnZKDesktopRegistry;
import org.zkoss.ganttz.util.script.IScriptsRegister;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class OrderPlanningController implements Composer {

    @Autowired
    private ViewSwitcher viewSwitcher;

    private Map<String, String[]> parameters;

    @Autowired
    private IOrderPlanningModel model;

    private Planner planner;

    @Autowired
    private CalendarAllocationController calendarAllocationController;

    @Autowired
    private EditTaskController editTaskController;

    private Order order;

    private List<ICommand<TaskElement>> additional = new ArrayList<ICommand<TaskElement>>();

    private Vbox orderElementFilter;
    private Datebox filterStartDateOrderElement;
    private Datebox filterFinishDateOrderElement;
    private BandboxMultipleSearch bdFiltersOrderElement;
    private Textbox filterNameOrderElement;

    public OrderPlanningController() {
        getScriptsRegister().register(ScriptsRequiredByResourceLoadPanel.class);
        ResourceAllocationController.registerNeededScripts();
    }

    private IScriptsRegister getScriptsRegister() {
        return OnZKDesktopRegistry.getLocatorFor(IScriptsRegister.class)
                .retrieve();
    }

    public void setOrder(Order order,
            ICommand<TaskElement>... additionalCommands) {
        Validate.notNull(additionalCommands);
        Validate.noNullElements(additionalCommands);
        this.order = order;
        this.additional = Arrays.asList(additionalCommands);
        if (planner != null) {
            updateConfiguration();
        }
    }

    public CalendarAllocationController getCalendarAllocationController() {
        return calendarAllocationController;
    }

    public ViewSwitcher getViewSwitcher() {
        return viewSwitcher;
    }

    @Override
    public void doAfterCompose(org.zkoss.zk.ui.Component comp) throws Exception {
        if (order == null) {
            throw new IllegalStateException("an order should have been set");
        }
        this.planner = (Planner) comp;
        String zoomLevelParameter = null;
        if ((parameters != null) && (parameters.get("zoom") != null)
                && !(parameters.isEmpty())) {
            zoomLevelParameter = parameters.get("zoom")[0];
        }
        if (zoomLevelParameter != null) {
            planner.setInitialZoomLevel(ZoomLevel
                    .getFromString(zoomLevelParameter));
        }
        planner.setAreContainersExpandedByDefault(Planner
                .guessContainersExpandedByDefault(parameters));

        orderElementFilter = (Vbox) planner.getFellow("orderElementFilter");
        // Configuration of the order filter
        org.zkoss.zk.ui.Component filterComponent = Executions
                .createComponents("/orders/_orderElementTreeFilter.zul",
                        orderElementFilter, new HashMap<String, String>());
        filterComponent.setVariable("treeController", this, true);
        filterStartDateOrderElement = (Datebox) filterComponent
                .getFellow("filterStartDateOrderElement");
        filterFinishDateOrderElement = (Datebox) filterComponent
                .getFellow("filterFinishDateOrderElement");
        bdFiltersOrderElement = (BandboxMultipleSearch) filterComponent
                .getFellow("bdFiltersOrderElement");
        filterNameOrderElement = (Textbox) filterComponent
                .getFellow("filterNameOrderElement");
        filterComponent.setVisible(true);

        updateConfiguration();
    }

    private void updateConfiguration() {
        model.setConfigurationToPlanner(planner, order, viewSwitcher,
                editTaskController, calendarAllocationController, additional);
        planner.updateSelectedZoomLevel();
    }

    public EditTaskController getEditTaskController() {
        return editTaskController;
    }

    public void setURLParameters(Map<String, String[]> parameters) {
        this.parameters = parameters;
    }

    public Order getOrder() {
        return model.getOrder();
    }

    public void onApplyFilter() {
        filterByPredicate(createPredicate());
    }

    private OrderElementPredicate createPredicate() {
        List<FilterPair> listFilters = (List<FilterPair>) bdFiltersOrderElement
                .getSelectedElements();
        Date startDate = filterStartDateOrderElement.getValue();
        Date finishDate = filterFinishDateOrderElement.getValue();
        String name = filterNameOrderElement.getValue();

        if (listFilters.isEmpty() && startDate == null && finishDate == null
                && name == null) {
            return null;
        }
        return new OrderElementPredicate(listFilters, startDate, finishDate,
                name);
    }

    private void filterByPredicate(final OrderElementPredicate predicate) {
        final IContext<?> context = planner.getContext();
        planner.setTaskListPredicate(new FilterAndParentExpandedPredicates(context) {
            @Override
            public boolean accpetsFilterPredicate(Task task) {
                if (predicate == null) {
                    return true;
                }
                TaskElement taskElement = (TaskElement) context.getMapper()
                        .findAssociatedDomainObject(task);
                return predicate.accepts(taskElement.getOrderElement());
            }

        });
    }

    public Constraint checkConstraintFinishDate() {
        return new Constraint() {
            @Override
            public void validate(org.zkoss.zk.ui.Component comp, Object value)
                    throws WrongValueException {
                Date finishDate = (Date) value;
                if ((finishDate != null)
                        && (filterStartDateOrderElement.getValue() != null)
                        && (finishDate.compareTo(filterStartDateOrderElement
                                .getValue()) < 0)) {
                    filterFinishDateOrderElement.setValue(null);
                    throw new WrongValueException(comp,
                            _("must be greater than start date"));
                }
            }

        };
    }

    public Constraint checkConstraintStartDate() {
        return new Constraint() {
            @Override
            public void validate(org.zkoss.zk.ui.Component comp, Object value)
                    throws WrongValueException {
                Date startDate = (Date) value;
                if ((startDate != null)
                        && (filterFinishDateOrderElement.getValue() != null)
                        && (startDate.compareTo(filterFinishDateOrderElement
                                .getValue()) > 0)) {
                    filterStartDateOrderElement.setValue(null);
                    throw new WrongValueException(comp,
                            _("must be lower than finish date"));
                }
            }
        };
    }

}

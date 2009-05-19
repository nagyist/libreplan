package org.navalplanner.web.resources;

import java.util.List;

import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.ICriterionType;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.OnlyOneVisible;
import org.navalplanner.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.GroupsModel;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleGroupsModel;
import org.zkoss.zul.api.Grid;
import org.zkoss.zul.api.Group;

/**
 * Controller for Criterions <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class CriterionAdminController extends GenericForwardComposer {

    private ICriterionsModel criterionsModel;

    private Component messagesContainer;

    private IMessagesForUser messagesForUser;

    private Grid listing;

    private Component editComponent;

    private Component createComponent;

    private OnlyOneVisible onlyOneVisible;

    public CriterionAdminController() {

    }

    private class GroupRenderer implements RowRenderer {
        public void render(Row row, java.lang.Object data) {
            if (data instanceof Criterion) {
                final Criterion criterion = (Criterion) data;
                Button editButton = new Button("Editar");
                editButton.setParent(row);
                editButton.setDisabled(!criterionsModel.getTypeFor(criterion)
                        .allowEditing());
                editButton.addEventListener("onClick", new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        goToEditForm(criterion);
                    }
                });
                new Label(criterion.getName()).setParent(row);
                Checkbox checkbox = new Checkbox();
                checkbox.setChecked(criterion.isActive());
                checkbox.setDisabled(true);
                checkbox.setParent(row);
            } else if (data instanceof ICriterionType) {
                final ICriterionType<?> type = (ICriterionType<?>) data;
                Div div = new Div();
                Button createButton = new Button("Engadir");
                createButton.setDisabled(!type.allowAdding());
                createButton.addEventListener("onClick", new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        goToCreateForm((ICriterionType<Criterion>) type);
                    }
                });
                div.appendChild(createButton);
                div.setParent(row);
                row.setSpans("3");
            } else {
                Group group = (Group) row;
                group.setLabel(data.toString());
                group.setSpans("3");
            }
        }

    }

    private void goToCreateForm(ICriterionType<Criterion> type) {
        onlyOneVisible.showOnly(createComponent);
        Util.reloadBindings(createComponent);
        criterionsModel.prepareForCreate(type);
    }

    private void goToEditForm(Criterion criterion) {
        onlyOneVisible.showOnly(editComponent);
        criterionsModel.prepareForEdit(criterion);
        Util.reloadBindings(editComponent);
    }

    public void setCriterionName(String name) {
        criterionsModel.setNameForCriterion(name);
    }

    public String getCriterionName() {
        return criterionsModel.getNameForCriterion();
    }

    public boolean isEditing() {
        return criterionsModel.isEditing();
    }

    public boolean isCriterionActive() {
        return criterionsModel.isCriterionActive();
    }

    public void setCriterionActive(boolean active) {
        criterionsModel.setCriterionActive(active);
    }

    public void save() {
        onlyOneVisible.showOnly(listing);
        try {
            criterionsModel.saveCriterion();
            reload();
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
        }
    }

    public void cancel() {
        onlyOneVisible.showOnly(listing);
    }

    private RowRenderer getRowRenderer() {
        return new GroupRenderer();
    }

    private GroupsModel getTypesWithCriterions() {
        List<ICriterionType<?>> types = criterionsModel.getTypes();
        Object[][] groups = new Object[types.size()][];
        int i = 0;
        for (ICriterionType<?> type : types) {
            groups[i] = criterionsModel.getCriterionsFor(type).toArray();
            i++;
        }
        return new SimpleGroupsModel(groups, asStrings(types), types.toArray());
    }

    private String[] asStrings(List<ICriterionType<?>> types) {
        String[] result = new String[types.size()];
        int i = 0;
        for (ICriterionType<?> criterionType : types) {
            result[i++] = criterionType.getName();
        }
        return result;
    }

    public Criterion getCriterion() {
        return criterionsModel.getCriterion();
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        onlyOneVisible = new OnlyOneVisible(listing, editComponent,
                createComponent);
        onlyOneVisible.showOnly(listing);
        comp.setVariable("controller", this, true);
        messagesForUser = new MessagesForUser(messagesContainer);
        listing = (Grid) comp.getFellow("listing");
        reload();
        listing.setRowRenderer(getRowRenderer());
    }

    private void reload() {
        listing.setModel(getTypesWithCriterions());
    }
}

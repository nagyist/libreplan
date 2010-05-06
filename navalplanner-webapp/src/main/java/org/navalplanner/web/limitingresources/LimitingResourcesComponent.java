/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galic
 *                    ia
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

package org.navalplanner.web.limitingresources;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalDate;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.planner.entities.LimitingResourceQueueElement;
import org.navalplanner.business.resources.entities.LimitingResourceQueue;
import org.zkoss.ganttz.IDatesMapper;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.ganttz.timetracker.zoom.IZoomLevelChangedListener;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.MenuBuilder;
import org.zkoss.ganttz.util.MenuBuilder.ItemAction;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zul.Div;
import org.zkoss.zul.impl.XulElement;

/**
 * This class wraps ResourceLoad data inside an specific HTML Div component.
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class LimitingResourcesComponent extends XulElement implements
        AfterCompose {

    public static LimitingResourcesComponent create(TimeTracker timeTracker,
            LimitingResourceQueue limitingResourceQueue) {
        return new LimitingResourcesComponent(timeTracker,
                limitingResourceQueue);
    }

    private final LimitingResourceQueue limitingResourceQueue;
    private final TimeTracker timeTracker;
    private transient IZoomLevelChangedListener zoomChangedListener;
    private List<Div> queueElementDivs = new ArrayList<Div>();

    private LimitingResourcesComponent(final TimeTracker timeTracker,
            final LimitingResourceQueue limitingResourceQueue) {
        this.limitingResourceQueue = limitingResourceQueue;
        this.timeTracker = timeTracker;
        createChildren(limitingResourceQueue, timeTracker.getMapper());
        zoomChangedListener = new IZoomLevelChangedListener() {

            @Override
            public void zoomLevelChanged(ZoomLevel detailLevel) {
                getChildren().clear();
                createChildren(limitingResourceQueue, timeTracker.getMapper());
                invalidate();
            }
        };
        this.timeTracker.addZoomListener(zoomChangedListener);
    }

    private void createChildren(LimitingResourceQueue limitingResourceQueue,
            IDatesMapper mapper) {
        List<Div> divs = createDivsForQueueElements(mapper,
                limitingResourceQueue.getLimitingResourceQueueElements());
        if (divs != null) {
            for (Div div : divs) {
                appendChild(div);
            }
            queueElementDivs.addAll(divs);
        }
    }

    public String getResourceName() {
        return limitingResourceQueue.getResource().getName();
    }

    private static List<Div> createDivsForQueueElements(
            IDatesMapper datesMapper,
            Set<LimitingResourceQueueElement> list) {
        List<Div> result = new ArrayList<Div>();

        for (LimitingResourceQueueElement queueElement : list) {
            validateQueueElement(queueElement);
            result.add(createDivForQueueElement(datesMapper, queueElement));
        }

        return result;
    }

    private static void validateQueueElement(
            LimitingResourceQueueElement queueElement) {
        if ((queueElement.getStartDate() == null)
                || (queueElement.getEndDate() == null)) {
            throw new ValidationException(_("Invalid queue element"));
        }
    }

    private void appendMenu(Div divElement) {
        if (divElement.getPage() != null) {
            MenuBuilder<Div> menuBuilder = MenuBuilder.on(divElement.getPage(),
                    queueElementDivs);
            menuBuilder.item(_("Unassign"), "/common/img/ico_borrar.png",
                    new ItemAction<Div>() {
                        @Override
                        public void onEvent(Div choosen, Event event) {
                            unnasign(choosen);
                        }
                    });
            divElement.setContext(menuBuilder.createWithoutSettingContext());
        }
    }

    // FIXME: Implement real unnasign operation
    private void unnasign(Div choosen) {
        choosen.detach();
    }

    private static Div createDivForQueueElement(IDatesMapper datesMapper,
            LimitingResourceQueueElement queueElement) {
        Div result = new Div();
        result.setClass("queue-element");

        result.setTooltiptext(queueElement.getLimitingResourceQueue()
                .getResource().getName());

        result.setLeft(forCSS(getStartPixels(datesMapper, queueElement)));
        result.setWidth(forCSS(getWidthPixels(datesMapper, queueElement)));

        return result;
    }

    private static int getWidthPixels(IDatesMapper datesMapper,
            LimitingResourceQueueElement loadPeriod) {
        LocalDate start = loadPeriod.getStartDate();
        LocalDate end = loadPeriod.getEndDate();
        return datesMapper
                .toPixels(toMilliseconds(end) - toMilliseconds(start));
    }

    private static long toMilliseconds(LocalDate localDate) {
        return localDate.toDateMidnight().getMillis();
    }

    private static String forCSS(int pixels) {
        return String.format("%dpx", pixels);
    }

    private static int getStartPixels(IDatesMapper datesMapper,
            LimitingResourceQueueElement queueElement) {
        return datesMapper.toPixels(queueElement.getStartDate().toDateMidnight()
                .toDate());
    }

    private void appendContextMenus() {
        for (Div each : queueElementDivs) {
            appendMenu(each);
        }
    }

    @Override
    public void afterCompose() {
        appendContextMenus();
    }

}

package customer.ai2code.handlers;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnElementRef;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnValue;
import com.sap.cds.ql.cqn.Modifier;
import com.sap.cds.ql.cqn.transformation.CqnTopLevelsTransformation;
import com.sap.cds.ql.cqn.transformation.CqnAncestorsTransformation;
import com.sap.cds.ql.cqn.transformation.CqnDescendantsTransformation;
import com.sap.cds.ql.cqn.transformation.CqnFilterTransformation;
import com.sap.cds.ql.cqn.transformation.CqnSearchTransformation;
import com.sap.cds.ql.cqn.transformation.CqnOrderByTransformation;
import com.sap.cds.ql.cqn.transformation.CqnTransformation;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.mainservice.MainService_;
import cds.gen.mainservice.TaskHierarchy_;
import cds.gen.mainservice.TaskHierarchy;

import static cds.gen.mainservice.MainService_.TASK_HIERARCHY;


@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceTaskHierarchyHandler implements EventHandler {
    private final PersistenceService db;

    MainServiceTaskHierarchyHandler(PersistenceService db) {
        this.db = db;
    }

    @Before(event = CqnService.EVENT_READ, entity = TaskHierarchy_.CDS_NAME)
    public void readTaskHierarchy(CdsReadEventContext event) {
        List<CqnTransformation> trafos = event.getCqn().transformations();
        List<TaskHierarchy> result = null;

        if (trafos.size() < 1) {
            return;
        }

        if (getTopLevels(trafos) instanceof CqnTopLevelsTransformation topLevels) {
            result = topLevels(topLevels, CQL.TRUE);
        } else if (trafos.get(0) instanceof CqnDescendantsTransformation descendants) {
            result = handleDescendants(descendants);
        } else if (trafos.get(0) instanceof CqnAncestorsTransformation ancestors) {
            if (trafos.size() == 2 && trafos.get(1) instanceof CqnTopLevelsTransformation topLevels) {
                result = handleAncestors(ancestors, topLevels);
            } else if (trafos.size() == 3 && trafos.get(2) instanceof CqnTopLevelsTransformation topLevels) {
                result = handleAncestors(ancestors, topLevels);
            }
        }

        setResult(event, result);
    }

    private CqnTopLevelsTransformation getTopLevels(List<CqnTransformation> trafos) {
        if (trafos.get(0) instanceof CqnTopLevelsTransformation topLevels) {
            return topLevels;
        } else if (trafos.size() == 2 && trafos.get(0) instanceof CqnOrderByTransformation
                && trafos.get(1) instanceof CqnTopLevelsTransformation topLevels) {
            return topLevels;
        }
        return null;
    }

    private void setResult(CdsReadEventContext event, List<TaskHierarchy> result) {
        if (!result.isEmpty()) {
            addDrillState(result);
        }

        event.setResult(result);
    }

    private void addDrillState(List<TaskHierarchy> ghs) {
        List<String> ids = ghs.stream().map(gh -> gh.getId()).toList();
        Set<String> parents = ghs.stream().map(gh -> gh.getParentId()).filter(p -> p != null)
                .collect(Collectors.toSet());
        CqnSelect q = Select.from(TASK_HIERARCHY).columns(gh -> gh.parent_ID().as("id"))
                .where(gh -> gh.parent_ID().in(ids));
        Set<Object> nonLeafs = db
                .run(q)
                .stream().map(r -> r.get("id")).collect(Collectors.toSet());

        for (TaskHierarchy gh : ghs) {
            String id = gh.getId();
            if (nonLeafs.contains(id)) {
                if (parents.contains(id)) {
                    gh.setDrillState("expanded");
                } else {
                    gh.setDrillState("collapsed");
                }
            } else {
                gh.setDrillState("leaf");
            }
        }
    }

    private CqnPredicate descendantsFilter(CqnDescendantsTransformation descendants) {
        CqnTransformation trafo = descendants.transformations().get(0);
        CqnPredicate start = ((CqnFilterTransformation) trafo).filter();
        CqnPredicate result = CQL.FALSE;
        if (descendants.keepStart()) {
            result = CQL.or(result, start);
        }
        CqnPredicate children = CQL.copy(start, new Modifier() {
            @Override
            public CqnValue ref(CqnElementRef ref) {
                return CQL.get(TaskHierarchy.PARENT_ID);
            }
        });
        result = CQL.or(result, children);

        return result;
    }

    private CqnPredicate ancestorsFilter(CqnAncestorsTransformation ancestors) {
        CqnTransformation trafo = ancestors.transformations().get(0);
        Select<TaskHierarchy_> inner = Select.from(TASK_HIERARCHY).columns(gh -> gh.ID());
        if (trafo instanceof CqnFilterTransformation filter) {
            inner.where(filter.filter());
        } else if (trafo instanceof CqnSearchTransformation search) {
            inner.search(search.search());
        }

        Select<TaskHierarchy_> outer = Select.from(TASK_HIERARCHY)
                .columns(gh -> gh.ID().as("i0"),
                        gh -> gh.parent().ID().as("i1"),
                        gh -> gh.parent().parent().ID().as("i2"),
                        gh -> gh.parent().parent().parent().ID().as("i3"),
                        gh -> gh.parent().parent().parent().parent().ID().as("i4"))
                .where(gh -> gh.ID().in(inner));

        Set<String> ancestorIds = new HashSet<>();
        db.run(outer).stream().forEach(r -> {
            addIfNotNull(ancestorIds, r, "i0");
            addIfNotNull(ancestorIds, r, "i1");
            addIfNotNull(ancestorIds, r, "i2");
            addIfNotNull(ancestorIds, r, "i3");
            addIfNotNull(ancestorIds, r, "i4");
        });

        return CQL.get(TaskHierarchy_.ID).in(ancestorIds.stream().toList());
    }

    private List<TaskHierarchy> handleDescendants(CqnDescendantsTransformation descendants) {
        CqnPredicate filter = descendantsFilter(descendants);
        CqnSelect childrenCQN = Select.from(TASK_HIERARCHY).where(filter);
        List<TaskHierarchy> nodes = db.run(childrenCQN).listOf(TaskHierarchy.class);

        connect(nodes);

        return nodes.stream().sorted(new Sorter()).toList();
    }

    private static void connect(List<TaskHierarchy> nodes) {
        Map<String, TaskHierarchy> lookup = new HashMap<>();
        nodes.forEach(gh -> lookup.put(gh.getId(), gh));
        nodes.forEach(gh -> gh.setParent(lookup.get(gh.getParentId())));
        nodes.forEach(gh -> gh.setDistanceFromRoot(distanceFromRoot(gh)));
    }

    private List<TaskHierarchy> handleAncestors(CqnAncestorsTransformation ancestors,
            CqnTopLevelsTransformation topLevels) {
        CqnPredicate filter = ancestorsFilter(ancestors);

        return topLevels(topLevels, filter);
    }

    private void addIfNotNull(Set<String> ancestorIds, Row r, String key) {
        String id = (String) r.get(key);
        if (id != null) {
            ancestorIds.add(id);
        }
    }

    private List<TaskHierarchy> topLevels(CqnTopLevelsTransformation topLevels, CqnPredicate filter) {
        return topLevels.levels() < 0 ? topLevelsAll(filter) : topLevelsLimit(topLevels, filter);
    }

    private List<TaskHierarchy> topLevelsLimit(CqnTopLevelsTransformation topLevels, CqnPredicate filter) {
        long limit = topLevels.levels();
        Map<String, TaskHierarchy> lookup = new HashMap<>();
        Map<Object, Long> expandLevels = topLevels.expandLevels();

        CqnSelect getRoots = Select.from(TASK_HIERARCHY).where(gh -> gh.parent_ID().isNull().and(filter));
        List<TaskHierarchy> roots = db.run(getRoots).listOf(TaskHierarchy.class);
        roots.forEach(root -> {
            root.setDistanceFromRoot(0l);
            lookup.put(root.getId(), root);
            List<String> parents = List.of(root.getId());
            for (long i = 1; i < limit; i++) {
                List<String> ps = parents;
                CqnSelect getChildren = Select.from(TASK_HIERARCHY)
                        .where(gh -> gh.parent_ID().in(ps).and(filter));
                List<TaskHierarchy> children = db.run(getChildren).listOf(TaskHierarchy.class);
                if (children.isEmpty()) {
                    break;
                }
                long dfr = i;
                parents = children.stream().peek(gh -> {
                    gh.setParent(lookup.get(gh.getParentId()));
                    gh.setDistanceFromRoot(dfr);
                    lookup.put(gh.getId(), gh);
                }).map(TaskHierarchy::getId).toList();
            }
        });

        if (!expandLevels.isEmpty()) {
            List<String> expandedIds = expandLevels.keySet().stream().map(key -> (String) key).toList();
            CqnSelect expandedCQN = Select.from(MainService_.TASK_HIERARCHY).where(gh -> CQL.and(filter,
                    CQL.or(gh.ID().in(expandedIds), gh.parent_ID().in(expandedIds))));

            List<TaskHierarchy> expanded = db.run(expandedCQN).listOf(TaskHierarchy.class);
            expanded.forEach(gh -> {
                if (!lookup.keySet().contains(gh.getId())) {
                    gh.setParent(lookup.get(gh.getParentId()));
                    gh.setDistanceFromRoot(distanceFromRoot(gh));
                    lookup.put(gh.getId(), gh);
                }
            });

        }

        return lookup.values().stream().sorted(new Sorter()).toList();
    }

    private List<TaskHierarchy> topLevelsAll(CqnPredicate filter) {
        CqnSelect allCqn = Select.from(TASK_HIERARCHY).where(filter);
        var all = db.run(allCqn).listOf(TaskHierarchy.class);

        connect(all);

        return all.stream().sorted(new Sorter()).toList();
    }

    private static long distanceFromRoot(TaskHierarchy gh) {
        long dfr = 0;
        while (gh.getParent() != null) {
            dfr++;
            gh = gh.getParent();
        }

        return dfr;
    }

    static class Sorter implements Comparator<TaskHierarchy> {

        @Override
        public int compare(TaskHierarchy gh1, TaskHierarchy gh2) {
            Deque<String> path1 = getPath(gh1);
            Deque<String> path2 = getPath(gh2);
            int res = 0;

            while (true) {
                if (path1.isEmpty()) {
                    return path2.isEmpty() ? 0 : -1;
                }
                if (path2.isEmpty()) {
                    return +1;
                }
                String last1 = path1.pop();
                String last2 = path2.pop();
                res = last1.compareTo(last2);
                if (res != 0) {
                    return res;
                }
            }
        }

        Deque<String> getPath(TaskHierarchy gh) {
            Deque<String> path = new ArrayDeque<>();
            do {
                path.push(gh.getName());
                gh = gh.getParent();
            } while (gh != null);

            return path;
        }
    }
}

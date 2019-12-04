package ccd.sdk.generator;

import ccd.sdk.types.Event;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventGenerator {
    public static void writeEvents(File root, ConfigBuilderImpl impl) {
        List<Event> events = impl.events.stream().map(x -> x.build()).collect(Collectors.toList());
        ImmutableListMultimap<String, Event> eventsByState = Multimaps.index(events, x -> "Open");

        File folder = new File(root.getPath(), "CaseEvent");
        folder.mkdir();
        for (String state : eventsByState.keys()) {
            Path output = Paths.get(folder.getPath(), state + ".json");

            Utils.writeFile(output, serialise(impl.caseType, eventsByState.get(state)));
        }
    }

    private static String serialise(String caseTypeId, List<Event> events) {
        int t = 1;
        List result = Lists.newArrayList();
        for (Event event : events) {
            Map<String, Object> data = Utils.getField(event.getId());
            result.add(data);
            data.put("Name", event.getName());
            data.put("Description", event.getDescription());
            if (event.getDisplayOrder() != -1) {
                data.put("DisplayOrder", event.getDisplayOrder());
            } else {
                data.put("DisplayOrder", t++);
            }
            data.put("CaseTypeID", caseTypeId);
            data.put("ShowSummary", "N");
            data.put("ShowEventNotes", "N");
            data.put("EndButtonLabel", "Save and continue");


            if (event.getPreState() != null) {
                data.put("PreConditionState(s)", event.getPreState());
            }
            data.put("PostConditionState", event.getPostState());
            data.put("SecurityClassification", "Public");

            if (event.getAboutToStartURL() != null) {
                data.put("CallBackURLAboutToStartEvent", formatUrl(event.getAboutToStartURL()));
                if (event.getRetries() != null) {
                    data.put("RetriesTimeoutURLAboutToStartEvent", event.getRetries());
                }
            }

            if (event.getAboutToSubmitURL() != null) {
                data.put("CallBackURLAboutToSubmitEvent", formatUrl(event.getAboutToSubmitURL()));
                if (event.getRetries() != null) {
                    data.put("RetriesTimeoutURLAboutToSubmitEvent", event.getRetries());
                }
            }

            if (event.getSubmittedURL() != null) {
                data.put("CallBackURLSubmittedEvent", formatUrl(event.getSubmittedURL()));
                if (event.getRetries() != null) {
                    data.put("RetriesTimeoutURLSubmittedEvent", event.getRetries());
                }
            }
        }

        return Utils.serialise(result);
    }

    private static String formatUrl(String url) {
        return "${CCD_DEF_CASE_SERVICE_BASE_URL}/callback" + url;
    }
}

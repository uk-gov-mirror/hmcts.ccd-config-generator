package uk.gov.hmcts.ccd.sdk;

import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import net.jodah.typetools.TypeResolver;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

class ConfigGenerator<T, S, R extends HasRole> {

  private final Reflections reflections;
  private final String basePackage;

  public ConfigGenerator(Reflections reflections, String basePackage) {
    this.reflections = reflections;
    this.basePackage = basePackage;
  }

  public void resolveConfig(File outputFolder) {
    Set<Class<? extends CCDConfig>> configTypes =
        reflections.getSubTypesOf(CCDConfig.class).stream()
            .filter(x -> !Modifier.isAbstract(x.getModifiers())).collect(Collectors.toSet());

    if (configTypes.isEmpty()) {
      throw new RuntimeException("Expected at least one CCDConfig implementation but none found. "
          + "Scanned: " + basePackage);
    }

    initOutputDirectory(outputFolder);

    for (Class<? extends CCDConfig> configType : configTypes) {
      Objenesis objenesis = new ObjenesisStd();
      CCDConfig config = objenesis.newInstance(configType);
      ResolvedCCDConfig resolved = resolveCCDConfig(config);
      File destination = Strings.isNullOrEmpty(resolved.environment) ? outputFolder
          : new File(outputFolder, resolved.environment);
      writeConfig(destination, resolved);
    }
  }

  @SneakyThrows
  public ResolvedCCDConfig<T, S, R> resolveCCDConfig(CCDConfig<T, S, R> config) {
    Class<?>[] typeArgs = TypeResolver.resolveRawArguments(CCDConfig.class, config.getClass());
    Set<S> allStates = Set.of(((Class<S>)typeArgs[1]).getEnumConstants());
    ConfigBuilderImpl builder = new ConfigBuilderImpl(typeArgs[0], allStates);
    config.configure(builder);
    List<Event> events = builder.getEvents();
    Map<Class, Integer> types = resolve(typeArgs[0], basePackage);
    return new ResolvedCCDConfig(typeArgs[0], typeArgs[1], typeArgs[2], builder, events, types,
        builder.environment, allStates);
  }

  private void writeConfig(File outputfolder, ResolvedCCDConfig config) {
    outputfolder.mkdirs();
    CaseEventGenerator.writeEvents(outputfolder, config.builder.caseType, config.events);
    CaseEventToFieldsGenerator.writeEvents(outputfolder, config.events, config.builder.caseType);
    ComplexTypeGenerator.generate(outputfolder, config.builder.caseType, config.types);
    CaseEventToComplexTypesGenerator.writeEvents(outputfolder, config.events);
    Table<String, R, String> eventPermissions = buildEventPermissions(config.builder,
        config.events, config.allStates);
    AuthorisationCaseEventGenerator.generate(outputfolder, eventPermissions,
        config.builder.caseType);
    AuthorisationCaseFieldGenerator.generate(outputfolder, config.builder.caseType, config.events,
        eventPermissions, config.builder.tabs, config.builder.workBasketInputFields,
        config.builder.workBasketResultFields, config.builder.searchInputFields,
            config.builder.searchResultFields, config.builder.roleHierarchy,
        config.builder.apiOnlyRoles, config.builder.explicitFields,
        config.builder.stateRoleHistoryAccess, config.builder.noFieldAuthRoles);
    CaseFieldGenerator
        .generateCaseFields(outputfolder, config.builder.caseType, config.typeArg, config.events,
            config.builder);
    generateJurisdiction(outputfolder, config.builder);
    generateCaseType(outputfolder, config.builder);
    FixedListGenerator.generate(outputfolder, config.types);
    StateGenerator.generate(outputfolder, config.builder.caseType, config.stateArg);
    AuthorisationCaseTypeGenerator.generate(outputfolder, config.builder.caseType, config.roleType);
    CaseTypeTabGenerator.generate(outputfolder, config.builder.caseType, config.builder);
    AuthorisationCaseStateGenerator.generate(outputfolder, config.builder.caseType, config.events,
        eventPermissions);
    WorkBasketGenerator.generate(outputfolder, config.builder.caseType, config.builder);
    SearchFieldAndResultGenerator.generate(outputfolder, config.builder.caseType, config.builder);
  }

  @SneakyThrows
  private void initOutputDirectory(File outputfolder) {
    if (outputfolder.exists() && outputfolder.isDirectory()) {
      MoreFiles.deleteRecursively(outputfolder.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
    }
    outputfolder.mkdirs();
  }

  private void generateCaseType(File outputfolder, ConfigBuilderImpl builder) {
    List<Map<String, Object>> fields = Lists.newArrayList();
    fields.add(Map.of(
        "LiveFrom", "01/01/2017",
        "ID", builder.caseType,
        "Name", builder.caseName,
        "Description", builder.caseDesc,
        "JurisdictionID", builder.jurId,
        "SecurityClassification", "Public"
    ));
    Path output = Paths.get(outputfolder.getPath(),"CaseType.json");
    JsonUtils.mergeInto(output, fields, new JsonUtils.AddMissing(), "ID");
  }

  private void generateJurisdiction(File outputfolder, ConfigBuilderImpl builder) {
    List<Map<String, Object>> fields = Lists.newArrayList();
    fields.add(ImmutableMap.of(
        "LiveFrom", "01/01/2017",
        "ID", builder.jurId,
        "Name", builder.jurName,
        "Description", builder.jurDesc
    ));
    Path output = Paths.get(outputfolder.getPath(),"Jurisdiction.json");
    JsonUtils.mergeInto(output, fields, new JsonUtils.AddMissing(), "ID");
  }

  public static Map<Class, Integer> resolve(Class dataClass, String basePackage) {
    Map<Class, Integer> result = Maps.newHashMap();
    resolve(dataClass, result, 0);
    result = Maps.filterKeys(result, x -> x.getPackageName().startsWith(basePackage));
    return result;
  }

  private static void resolve(Class dataClass, Map<Class, Integer> result, int level) {
    for (java.lang.reflect.Field field : ReflectionUtils.getFields(dataClass)) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      Class c = getComplexType(dataClass, field);
      if (null != c && !c.equals(dataClass)) {
        if (!result.containsKey(c) || result.get(c) < level) {
          result.put(c, level);
        }
        resolve(c, result, level + 1);
      }
    }
  }

  public static Class getComplexType(Class c, Field field) {
    if (Collection.class.isAssignableFrom(field.getType())) {
      ParameterizedType type = (ParameterizedType) TypeResolver.reify(field.getGenericType(), c);
      if (type.getActualTypeArguments()[0] instanceof ParameterizedType) {
        type = (ParameterizedType) type.getActualTypeArguments()[0];
      }
      return (Class) type.getActualTypeArguments()[0];
    }
    return field.getType();
  }

  Table<String, R, String> buildEventPermissions(
      ConfigBuilderImpl<T, S, R> builder, List<Event<T, R, S>> events, Set<S> allStates) {


    Table<String, R, String> eventRolePermissions = HashBasedTable.create();
    for (Event<T, R, S> event : events) {
      // Add any state based role permissions unless event permits only explicit grants.
      if (!event.isExplicitGrants()) {
        // If Event is for all states, then apply each state's state level permissions.
        Set<S> keys = event.getPreState().equals(allStates)
            ? builder.stateRolePermissions.rowKeySet()
            : event.getPostState();
        for (S key : keys) {
          Map<R, String> roles = builder.stateRolePermissions.row(key);
          for (R role : roles.keySet()) {
            eventRolePermissions.put(event.getId(), role, roles.get(role));
          }
        }

        // Add any case history access
        SetMultimap<S, R> stateRoleHistoryAccess = builder.stateRoleHistoryAccess;
        for (S s : event.getPostState()) {
          if (stateRoleHistoryAccess.containsKey(s)) {
            for (R role : stateRoleHistoryAccess.get(s)) {
              eventRolePermissions.put(event.getId(), role, "R");
            }
          }
        }
      }
      // Set event level permissions, overriding state level where set.
      SetMultimap<R, Permission> grants = event.getGrants();
      for (R role : grants.keySet()) {
        eventRolePermissions.put(event.getId(), role,
            Permission.toCCDPerm(grants.get(role)));
      }
    }
    return eventRolePermissions;
  }
}

package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.MapType;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * thrift
 *  HashMap (see https://thrift.apache.org/docs/types#containers)
 */
public class MapParam extends NamedTypedValue<MapType, List<PairParam>>{

    private Integer minSize;
    private Integer maxSize;

    public MapParam(String name, MapType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        return getValue().stream().map(i-> {
            try {
                return new AbstractMap.SimpleEntry<>(i.getValue().getKey().newInstance(), i.getValue().getValue().newInstance());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(String.format("MapParam: could not create new instance for key and value (%s,%s)",
                        i.getValue().getKey().toString(), i.getValue().getValue().getType()));
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.MAP;
        if (getValue()!=null){
            dto.innerContent = getValue().stream().map(s->s.getDto()).collect(Collectors.toList());
        }
        if (minSize != null)
            dto.minSize = Long.valueOf(minSize);
        if (maxSize != null)
            dto.maxSize = Long.valueOf(maxSize);
        return dto;
    }

    @Override
    public MapParam copyStructure() {
        return new MapParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent!= null && !dto.innerContent.isEmpty()){
            PairParam t = getType().getTemplate();
            List<PairParam> values = dto.innerContent.stream().map(s-> {
                PairParam c = (PairParam) t.copyStructureWithProperties();
                c.setValueBasedOnDto(s);
                return c;
            }).collect(Collectors.toList());
            setValue(values);
        }

    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        if (instance == null) return;
        PairParam t = getType().getTemplate();
        List<PairParam> values = new ArrayList<>();
        for (Object e : ((Map) instance).entrySet()){
            PairParam copy = (PairParam) t.copyStructureWithProperties();
            copy.setValueBasedOnInstance(e);
            values.add(copy);
        }
        setValue(values);
    }

    @Override
    public void setValueBasedOnInstanceOrJson(Object json) throws JsonProcessingException {
        Object instance = json;
        if (json instanceof String)
            instance = parseValueWithJson((String) json);

        if (instance == null){
            setValue(null); return;
        }

        if (!isValidInstance(instance))
            throw new RuntimeException("cannot parse Map param "+getName()+" with the type "+json.getClass().getName());

        PairParam t = getType().getTemplate();
        List<PairParam> values = new ArrayList<>();
        for (Object e : ((Map) instance).entrySet()){
            PairParam copy = (PairParam) t.copyStructureWithProperties();
            copy.setValueBasedOnInstanceOrJson(e);
            values.add(copy);
        }
        setValue(values);
    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava) {
        String fullName = getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        List<String> codes = new ArrayList<>();
        String var = CodeJavaOrKotlinGenerator.oneLineInstance(isDeclaration, doesIncludeName, fullName, variableName, null, isJava);
        CodeJavaOrKotlinGenerator.addCode(codes, var, indent);
        if (getValue() == null) return codes;
        CodeJavaOrKotlinGenerator.addCode(codes, "{", indent);
        // new map
        CodeJavaOrKotlinGenerator.addCode(codes,
                CodeJavaOrKotlinGenerator.setInstance(
                        variableName,
                        CodeJavaOrKotlinGenerator.newMap(
                            isJava,
                            getType().getTemplate().getType().getFirstTemplate().getType().getTypeNameForInstanceInJavaOrKotlin(isJava),
                            getType().getTemplate().getType().getSecondTemplate().getType().getTypeNameForInstanceInJavaOrKotlin(isJava)),
                    isJava), indent+1);
        int index = 0;
        for (PairParam e: getValue()){
            String eKeyVarName = CodeJavaOrKotlinGenerator.handleVariableName(variableName+"_key_"+index);
            if (e.getValue().getKey() == null)
                throw new RuntimeException("key should not been null");
            codes.addAll(e.getValue().getKey().newInstanceWithJavaOrKotlin(true, true, eKeyVarName, indent+1, isJava));
            String eValueVarName = CodeJavaOrKotlinGenerator.handleVariableName(variableName+"_value_"+index);
            if (e.getValue().getValue() == null)
                throw new RuntimeException("value should not been null");
            codes.addAll(e.getValue().getValue().newInstanceWithJavaOrKotlin(true, true, eValueVarName, indent+1, isJava));
            CodeJavaOrKotlinGenerator.addCode(codes, variableName+".put("+eKeyVarName+","+eValueVarName+");", indent+1);
            index++;
        }

        CodeJavaOrKotlinGenerator.addCode(codes, "}", indent);
        return codes;
    }



    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        List<String> codes = new ArrayList<>();
        if (getValue() == null){
            CodeJavaOrKotlinGenerator.addCode(codes, CodeJavaOrKotlinGenerator.junitAssertNull(responseVarName, isJava), indent);
            return codes;
        }
        CodeJavaOrKotlinGenerator.addCode(codes, CodeJavaOrKotlinGenerator.junitAssertEquals(String.valueOf(getValue().size()), CodeJavaOrKotlinGenerator.withSize(responseVarName), isJava), indent);

        if (maxAssertionForDataInCollection == 0)
            return codes;

        if (doAssertion(getType().getTemplate().getType().getFirstTemplate())){
            List<Integer> nvalue = null;
            if (maxAssertionForDataInCollection > 0 && getValue().size() > maxAssertionForDataInCollection){
                nvalue = CodeJavaOrKotlinGenerator.randomNInt(getValue().size(), maxAssertionForDataInCollection);
            }else
                nvalue = IntStream.range(0, getValue().size()).boxed().collect(Collectors.toList());

            for (int index : nvalue){
                PairParam e = getValue().get(index);
                String key = e.getValue().getKey().getValueAsJavaString();
                if (key == null)
                    throw new RuntimeException("key is null");
                String eValueVarName = responseVarName+".get("+key+")";
                if (e.getValue().getValue() == null)
                    throw new RuntimeException("value should not been null");
                codes.addAll(e.getValue().getValue().newAssertionWithJavaOrKotlin(indent, eValueVarName, maxAssertionForDataInCollection, isJava));
            }

        }else{
            SimpleLogger.error("ERROR: do not support to generate assertions for Map with key :"+getType().getTemplate().getValue().getKey().getType().getFullTypeName());
        }

        return codes;
    }

    private boolean doAssertion(NamedTypedValue key){
        return key instanceof PrimitiveOrWrapperParam || key instanceof EnumParam || key instanceof StringParam;
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }

    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        if (this.minSize != null && this.minSize >= minSize)
            return;
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof MapParam){
            ((MapParam)copy).setMinSize(minSize);
            ((MapParam)copy).setMaxSize(maxSize);
        }
    }
}

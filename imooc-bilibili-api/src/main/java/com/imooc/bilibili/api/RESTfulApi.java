package com.imooc.bilibili.api;


import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class RESTfulApi {

    private final Map<String, Map<String,Object>> dataMap;

    public RESTfulApi(){
        dataMap = new HashMap<>();
        for(int i = 0 ; i < 3 ; i++){
            HashMap<String,Object> map = new HashMap<>();
            map.put("id",i);
            map.put("name","name"+i);
            dataMap.put(String.valueOf(i),map);
        }
    }

    @GetMapping("/objects/{id}")
    public Map<String,Object> getObjects(@PathVariable String id){
        return dataMap.get(id);
    }

    @DeleteMapping("/objects/{id}")
    public String deleteObjects(@PathVariable String id){
        dataMap.remove(id);
        return "删除成功";
    }

    @PostMapping("/objects")
    public String postObjects(@RequestBody Map<String,Object> map){
        Integer[] array = dataMap.keySet().stream().map(Integer::parseInt).collect(Collectors.toList()).toArray(new Integer[0]);
        Arrays.sort(array);
        int nextId = array[array.length-1]+1;
        dataMap.put(String.valueOf(nextId),map);
        return "添加成功";
    }

    @PutMapping("/objects")
    public String putObjects(@RequestBody Map<String,Object> map){
        String key = map.get("id").toString();
        if(!dataMap.containsKey(key)){
            Integer[] array = dataMap.keySet().stream().map(Integer::parseInt).collect(Collectors.toList()).toArray(new Integer[0]);
            Arrays.sort(array);
            int nextId = array[array.length-1]+1;
            dataMap.put(String.valueOf(nextId),map);
        }else{
            dataMap.put(key,map);
        }
        return "修改成功";
    }

}
























































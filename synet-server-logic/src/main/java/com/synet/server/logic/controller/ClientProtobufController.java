package com.synet.server.logic.controller;

import com.google.protobuf.GeneratedMessageV3;
import com.synet.server.logic.protobuf.TestOuterClass;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ClientProtobufController {

//    @FeignClient(name = "FEIGN-SERVICE2-TEST", fallback = FeignTestProtoFallback.class, configuration = MyProtoFeignConfiguration.class)
//    public interface FeignProtoTestClient {
//        @RequestMapping(value = "/replyProto", method = POST, consumes = "application/x-protobuf", produces = "application/x-protobuf")
//        HeaderReply requestMessage(@RequestParam("name") String name);
//    }



    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Welcome to reactive world ~");
    }


    @RequestMapping(value = "/test", method = POST, consumes = "application/x-protobuf", produces = "application/x-protobuf")
    public Mono<TestOuterClass.Test> test() {
        TestOuterClass.Test test = TestOuterClass.Test.newBuilder().setName("123").setPassword("456").build();
        return Mono.just(test);
    }
}

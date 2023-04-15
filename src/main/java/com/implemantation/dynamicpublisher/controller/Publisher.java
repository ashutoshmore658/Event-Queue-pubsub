package com.implemantation.dynamicpublisher.controller;

import com.implemantation.dynamicpublisher.Service.PublisherRepo;
import com.implemantation.dynamicpublisher.Service.SignHashRepo;
import com.implemantation.dynamicpublisher.dto.PublisherItem;
import com.implemantation.dynamicpublisher.dto.RequestMessage;
import com.implemantation.dynamicpublisher.dto.SignHashItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.List;

@RestController
@RequestMapping("/rest/api")
public class Publisher {

    private final Object $lock = new Object[0];

    @Autowired
    private PublisherRepo repo1;

    @Autowired
    private SignHashRepo repo2;

//    @Autowired
//    private KafkaTemplate<String,RequestMessage> kafkaTemplate;
    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody RequestMessage requestMessage) throws UnsupportedEncodingException {

        System.out.println("Message Before Sending ->\n" + requestMessage);

        byte[] sign;
        CryptoController controller;
        String msgHash;
        PublicKey publicKey;
        synchronized ($lock) {
            controller = new CryptoController();

            String userid = requestMessage.getUser_id();
            String topic = requestMessage.getTopic_id();

            String eventQ = userid + "#" + topic;
            String eqHash = controller.getHash(eventQ, "SHA3-256");

            if (eqHash == null) {
                System.out.println("An Error Occurred while computing Hash");
                return ResponseEntity.ok("Error Publishing Message");
            }

            String msg = requestMessage.getMsg();
            msgHash = controller.getHash(msg, "SHA3-512");

            sign = controller.signAndHash(msgHash);

            if (sign == null) {
                System.out.println("Null Signature Found");
                return ResponseEntity.ok("Error Publishing Message");
            }
            publicKey = controller.getPubKey();

            repo1.save(new PublisherItem(eqHash, msg));
            System.out.println("Successfully added to Database");

            //kafkaTemplate.send(requestMessage.getTopic_id(), requestMessage);

            repo2.save(new SignHashItem(eqHash, sign, publicKey.getEncoded()));
            System.out.println("Successfully Created Digitally Signed Hash");
        }

//        boolean flag = controller.verify(msgHash.getBytes(), sign, publicKey.getEncoded());
//        System.out.println("Verification Results " + flag);

        //System.out.print("Digital Signature : " + controller.convertToHex(sign));
        return ResponseEntity.ok("Message Published Successfully.");

    }
    @GetMapping("/view")
    public List<PublisherItem> fetchData(@RequestParam("topic_id") String topic_id){
        List<PublisherItem> publisherItemList=repo1.findByTopicId(topic_id);

        return publisherItemList;
    }
}


//            boolean flag = controller.verify(msg.getBytes(),sign,publicKey);
//            System.out.println("Verification Results "+flag);
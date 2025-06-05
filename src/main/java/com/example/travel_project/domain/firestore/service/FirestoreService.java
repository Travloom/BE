package com.example.travel_project.domain.firestore.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    public String savePlanData(String planId, String document, Object data) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<WriteResult> result = db
                .collection("travloom")
                .document("plan")
                .collection(planId)
                .document(document)
                .set(data);

        return result.get().getUpdateTime().toString();
    }
}

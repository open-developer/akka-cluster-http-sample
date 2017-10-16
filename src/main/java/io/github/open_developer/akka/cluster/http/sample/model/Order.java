package io.github.open_developer.akka.cluster.http.sample.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(of = "items")
public class Order implements Serializable {

    private static final long serialVersionUID = 4831253828621786123L;
    private int orderId;
    private List<Integer> items;
    private int userId;
}

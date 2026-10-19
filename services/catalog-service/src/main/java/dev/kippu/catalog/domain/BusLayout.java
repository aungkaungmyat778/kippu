package dev.kippu.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bus_layouts")
public class BusLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "column_labels", nullable = false, length = 20)
    private String columnLabels;

    @Column(name = "aisle_after", nullable = false)
    private int aisleAfter;

    @OneToMany(mappedBy = "layout")
    @OrderBy("rowNo ASC, colLabel ASC")
    private List<LayoutSeat> seats = new ArrayList<>();

    protected BusLayout() {
        // required by JPA
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getAisleAfter() {
        return aisleAfter;
    }

    public List<LayoutSeat> getSeats() {
        return seats;
    }

    /** Column labels in seat order, for example [A, B, C, D]. */
    public List<String> columns() {
        return List.of(columnLabels.split(","));
    }

    public int seatCount() {
        return rowCount * columns().size();
    }
}

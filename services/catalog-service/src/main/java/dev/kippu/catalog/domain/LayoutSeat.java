package dev.kippu.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "layout_seats")
public class LayoutSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_id", nullable = false)
    private BusLayout layout;

    @Column(name = "seat_no", nullable = false, length = 5)
    private String seatNo;

    @Column(name = "row_no", nullable = false)
    private int rowNo;

    @Column(name = "col_label", nullable = false, length = 2)
    private String colLabel;

    protected LayoutSeat() {
        // required by JPA
    }

    public String getSeatNo() {
        return seatNo;
    }

    public int getRowNo() {
        return rowNo;
    }

    public String getColLabel() {
        return colLabel;
    }
}

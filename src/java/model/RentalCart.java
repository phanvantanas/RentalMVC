package model;
import java.io.Serializable;import java.time.*;import java.util.*;
public class RentalCart implements Serializable {
 private final Map<Integer,Integer> quantities=new LinkedHashMap<>();
 private LocalDate start,end;private String pickupTime="08:00",returnTime="17:00";
 public Map<Integer,Integer> getQuantities(){return new LinkedHashMap<>(quantities);}
 public int getCount(){return quantities.values().stream().mapToInt(Integer::intValue).sum();}
 public LocalDate getStart(){return start;}public LocalDate getEnd(){return end;}public String getPickupTime(){return pickupTime;}public String getReturnTime(){return returnTime;}
 public String getStartLabel(){return start==null?"":start.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
 public String getEndLabel(){return end==null?"":end.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
 public long getDays(){return start==null||end==null?0:java.time.temporal.ChronoUnit.DAYS.between(start,end);}
 public int getDiscount(){long n=getDays();return n>=7?20:n>=5?15:n>=3?10:0;}
 public void add(int id,int quantity,LocalDate from,LocalDate to,String pickup,String dropoff){
  validateDates(from,to);validateTime(pickup);validateTime(dropoff);
  if(id<=0||quantity<1||quantity>10)throw new IllegalArgumentException("Số lượng phải từ 1 đến 10.");
  if(!quantities.isEmpty()&&(!from.equals(start)||!to.equals(end)||!pickup.equals(pickupTime)||!dropoff.equals(returnTime)))throw new IllegalArgumentException("Giỏ hàng đang có lịch thuê khác. Hãy hoàn tất hoặc xóa giỏ cũ trước khi chọn lịch mới.");
  int total=quantities.getOrDefault(id,0)+quantity;
  if(total>10||getCount()+quantity>50||(!quantities.containsKey(id)&&quantities.size()>=20))throw new IllegalArgumentException("Giỏ hàng tối đa 20 mẫu, 50 thiết bị và 10 thiết bị mỗi mẫu.");
  start=from;end=to;pickupTime=pickup;returnTime=dropoff;quantities.put(id,total);
 }
 public void update(int id,int quantity){if(!quantities.containsKey(id)||quantity<1||quantity>10||getCount()-quantities.get(id)+quantity>50)throw new IllegalArgumentException("Số lượng không hợp lệ.");quantities.put(id,quantity);}
 public void remove(int id){quantities.remove(id);if(quantities.isEmpty())clear();}
 public void clear(){quantities.clear();start=null;end=null;}
 public static void validateDates(LocalDate start,LocalDate end){if(start==null||end==null||start.isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))||!end.isAfter(start)||java.time.temporal.ChronoUnit.DAYS.between(start,end)>365)throw new IllegalArgumentException("Ngày thuê không hợp lệ; thời gian thuê tối đa 365 ngày.");}
 public static void validateTime(String value){if(value==null||!value.matches("(0[89]|1[0-8]):00"))throw new IllegalArgumentException("Giờ nhận/trả không hợp lệ.");}
}

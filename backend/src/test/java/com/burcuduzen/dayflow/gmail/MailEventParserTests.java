package com.burcuduzen.dayflow.gmail;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class MailEventParserTests {
    final MailEventParser parser=new MailEventParser();
    final ZonedDateTime sent=ZonedDateTime.of(2026,10,12,10,0,0,0,ZoneId.of("Europe/Istanbul"));
    final Instant now=sent.toInstant();
    @Test void tomorrowUsesMailDateAndConfirmedMeetingMayBeAutomatic(){
        var d=parser.parse("Proje toplantısı","Yarın saat 14:30 toplantı yapılacaktır. Süre 45 dakika.",sent,now);
        assertTrue(d.automatic());assertEquals(13,d.startAt().getDayOfMonth());assertEquals(14,d.startAt().getHour());assertEquals(45,Duration.between(d.startAt(),d.endAt()).toMinutes());
    }
    @Test void vagueInvitationHasNoInventedStart(){
        var d=parser.parse("Görüşme","Yarın öğleden sonra gelebilirsiniz.",sent,now);
        assertFalse(d.automatic());assertNull(d.startAt());
    }
    @Test void proposalCancellationAndNegationRequireReview(){
        for(String body:new String[]{"Yarın 14:30 görüşelim mi?","Yarın 14:30 toplantı iptal edildi.","Yarın 14:30 toplantı kesinleşmedi.","Yarın 14:30 toplantı onaylanmadı."}) assertFalse(parser.parse("Toplantı",body,sent,now).automatic());
    }
    @Test void multipleTimesAndPastMeetingRequireReview(){
        assertFalse(parser.parse("Toplantı","Yarın 14:30 veya 15:00 toplantı yapılacaktır.",sent,now).automatic());
        assertFalse(parser.parse("Toplantı","Yarın 14:30 toplantı yapılacaktır.",sent,now.plus(Duration.ofDays(3))).automatic());
    }
    @Test void irrelevantMailIsIgnored(){assertNull(parser.parse("İndirim","Yarın 14:30 indirim başlıyor",sent,now));}
}

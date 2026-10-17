package br.com.casacesar.luz;
import java.util.function.LongSupplier;
/** Testable ordering of the recovery plan and the explicit power-off request. */
final class CycleFlow {
    interface Recovery {void prepare(long at) throws Exception;boolean schedule();void arm();void clear();}
    interface CutPower {void run() throws Exception;}
    static void start(Recovery recovery,CutPower off,LongSupplier now) throws Exception {
        recovery.prepare(now.getAsLong()+60000);
        if(!recovery.schedule()){recovery.clear();throw new Exception("O Android não aceitou o religamento de segurança; a tomada não foi desligada.");}
        Exception failure=null;try{off.run();}catch(Exception e){failure=e;}
        // Even an ambiguous timeout retains recovery; never abandon a socket that might already be off.
        recovery.prepare(now.getAsLong()+10000);recovery.schedule();recovery.arm();
        if(failure!=null)throw new Exception("Não foi possível confirmar o desligamento. A tentativa de religar em 10 s foi mantida.");
    }
}

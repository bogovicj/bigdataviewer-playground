package sc.fiji.bdvpg.spimdata.importer;

import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.BdvService;

import java.io.File;
import java.util.function.Function;

public class SpimDataOpener implements Runnable, Function<File, AbstractSpimData> {

    AbstractSpimData spimData;

    File f;

    public SpimDataOpener( File f) {
        this.f = f;
    }

    public SpimDataOpener( String filePath) {
        this.f = new File(filePath);
    }

    @Override
    public void run() {
        apply( f ); // open and register with BdvService
    }

    public AbstractSpimData get() {
        return apply(f);
    }

    @Override
    public AbstractSpimData apply(File file) {
        AbstractSpimData sd = null;
        try {
            sd = new XmlIoSpimDataMinimal().load(file.getAbsolutePath());
            BdvService.getSourceService().register(sd);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
        return sd;
    }


}

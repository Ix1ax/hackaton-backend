package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ixlax.hackaton.api.publicapi.dto.CameraAlertDto;
import ru.ixlax.hackaton.domain.entity.Camera;
import ru.ixlax.hackaton.domain.repository.CameraRepo;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CameraService {
    private final CameraRepo cameras;
    private final SseHub sse;

    public Optional<Camera> pickFor(double lat, double lng, String regionCode) {
        List<Camera> list = (regionCode==null) ? cameras.findAll() : cameras.findByRegionCode(regionCode);
        return list.stream()
                .map(c -> new Object(){ Camera cam = c; double d = distanceMeters(lat,lng,c.getLat(),c.getLng()); })
                .filter(x -> x.d <= (x.cam.getRadiusM()==null ? 300 : x.cam.getRadiusM()))
                .min(Comparator.comparingDouble(x -> x.d))
                .map(x -> x.cam);
    }

    public void publishAlert(String incidentExtId, Camera cam, double lat, double lng){
        String url = cam.getPublicUrl()!=null && !cam.getPublicUrl().isBlank() ? cam.getPublicUrl() : cam.getUrl();
        var dto = new CameraAlertDto(System.currentTimeMillis(), incidentExtId, cam.getId(), cam.getName(), url, lat, lng);
        sse.publishCameraAlert(dto);
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2){
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
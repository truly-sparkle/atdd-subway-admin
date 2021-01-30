package nextstep.subway.line.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.domain.Section;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.dto.StationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LineService {

    private final LineRepository lineRepository;
    private final StationService stationService;

    public LineService(LineRepository lineRepository, StationService stationService) {
        this.lineRepository = lineRepository;
        this.stationService = stationService;
    }

    public LineResponse saveLine(LineRequest lineRequest) {
        Station upStation = stationService.findById(lineRequest.getUpStationId());
        Station downStation = stationService.findById(lineRequest.getDownStationId());
        Line persistLine = lineRepository.save(
            new Line(lineRequest.getName(), lineRequest.getColor(), upStation, downStation,
                lineRequest.getDistance()));
        List<StationResponse> stations = getStationResponses(persistLine);
        return LineResponse.of(persistLine, stations);
    }

    @Transactional(readOnly = true)
    public List<LineResponse> findAllLines() {
        List<Line> lines = lineRepository.findAll();

        return lines.stream()
            .map(line -> {
                List<StationResponse> stationResponses = getStationResponses(line);
                return LineResponse.of(line, stationResponses);
            })
            .collect(Collectors.toList());
    }

    public LineResponse findById(Long lineId) {
        Line line = findLineById(lineId);
        List<StationResponse> stationResponses = getStationResponses(line);
        return LineResponse.of(line, stationResponses);
    }

    public LineResponse update(Long lineId, LineRequest lineRequest) {
        Station upStation = stationService.findById(lineRequest.getUpStationId());
        Station downStation = stationService.findById(lineRequest.getDownStationId());
        Line lineById = findLineById(lineId);
        lineById.update(lineRequest.toLine(), upStation, downStation, lineRequest.getDistance());
        List<StationResponse> stationResponses = getStationResponses(lineById);
        return LineResponse.of(lineById, stationResponses);
    }

    private Line findLineById(Long lineId) {
        return lineRepository.findById(lineId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 LINE 입니다."));
    }

    public Long delete(Long lineId) {
        Line lineById = findLineById(lineId);
        lineRepository.delete(lineById);
        return lineById.getId();
    }

    private List<StationResponse> getStationResponses(Line line) {
        if (line.getSections().isEmpty()) {
            return Collections.emptyList();
        }

        List<Station> stations = new ArrayList<>();
        Station downStation = line.findFirstStation();
        stations.add(downStation);

        while (downStation != null) {
            Optional<Section> nextLineStation = line.getSectionByUpStation(downStation);
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getDownStation();
            stations.add(downStation);
        }

        return stations.stream()
            .map(StationResponse::of)
            .collect(Collectors.toList());
    }

    public void addSection(Long lineId, SectionRequest sectionRequest) {
        Line line = findLineById(lineId);
        Station upStation = stationService.findById(sectionRequest.getUpStationId());
        Station downStation = stationService.findById(sectionRequest.getDownStationId());
        line.addSection(new Section(line, upStation, downStation, sectionRequest.getDistance()));
    }
}

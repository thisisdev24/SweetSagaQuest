package SweetSagaQuestGame;

import com.almasb.fxgl.entity.component.Component;

public class CandyComponent extends Component {
    private String candyType;

    public CandyComponent(String candyType) {
        this.candyType = candyType;
    }

    public String getCandyType() {
        return candyType;
    }

    public void setCandyType(String candyType) {
        this.candyType = candyType;
    }
}

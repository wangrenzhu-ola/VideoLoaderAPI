//
//  RoomListViewCell.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/9/3.
//

import UIKit

class RoomListViewCell: UICollectionViewCell {
    public lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.backgroundColor = .red
        label.textColor = .blue
        label.textAlignment = .center
        label.font = UIFont.boldSystemFont(ofSize: 16)
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        _loadSubview()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func _loadSubview() {
        addSubview(titleLabel)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        titleLabel.frame = bounds
    }
}
